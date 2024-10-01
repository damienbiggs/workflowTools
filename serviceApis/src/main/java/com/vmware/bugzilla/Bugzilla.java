package com.vmware.bugzilla;

import com.vmware.AbstractService;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugKnobType;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.chrome.ChromeDevTools;
import com.vmware.chrome.SsoClient;
import com.vmware.chrome.domain.ApiRequest;
import com.vmware.chrome.domain.ApiResponse;
import com.vmware.config.section.SsoConfig;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.InternalServerException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.ThreadUtils;
import com.vmware.util.UrlUtils;
import com.vmware.xmlrpc.CookieAwareXmlRpcClient;
import com.vmware.xmlrpc.MapObjectConverter;
import com.vmware.xmlrpc.RuntimeXmlRpcException;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.vmware.http.cookie.ApiAuthentication.trello;
import static java.lang.String.format;


/**
 * Used for http calls against Bugzilla. Has to support bugzilla 3 version so can't use a REST API.
 * That's what you get for customizing Bugzilla, a brain dead decision.
 */
public class Bugzilla extends AbstractService {

    private final HttpConnection connection;
    private final CookieAwareXmlRpcClient xmlRpcClient;
    private final MapObjectConverter mapConverter;
    private final int testBugNumber;
    private final boolean bugzillaSso;
    private final SsoConfig ssoConfig;
    private final String ssoButtonId;

    public Bugzilla(String bugzillaUrl, String username, int testBugNumber, boolean bugzillaSso, SsoConfig ssoConfig, String ssoButtonId) {
        super(bugzillaUrl, "xmlrpc.cgi", ApiAuthentication.bugzilla_cookie, username);
        this.testBugNumber = testBugNumber;
        this.bugzillaSso = bugzillaSso;
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);
        xmlRpcClient = new CookieAwareXmlRpcClient(apiUrl, connection.getCookieFileStore());
        mapConverter = new MapObjectConverter();
        this.ssoConfig = ssoConfig;
        this.ssoButtonId = ssoButtonId;
    }

    public List<Bug> getBugsForQuery(String savedQueryToRun) {
        Map values = xmlRpcClient.executeCall("Search.run_saved_query", getUsername(), savedQueryToRun);
        Object[] bugs = (Object[]) values.get("bugs");
        List<Bug> bugList = new ArrayList<>();
        for (Object bug : bugs) {
            Map bugValues = (Map) bug;
            bugValues.put("web_url", constructFullBugUrl((Integer) bugValues.get("bug_id")));
            bugList.add(mapConverter.fromMap(bugValues, Bug.class));
        }
        return bugList;
    }

    public Bug getBugById(int id) {
        Map values = xmlRpcClient.executeCall("Bug.show_bug", id);
        values.put("web_url", constructFullBugUrl(id));
        return mapConverter.fromMap(values, Bug.class);
    }

    public Bug getBugByIdWithoutException(int id) {
        try {
            return getBugById(id);
        } catch (NotFoundException nfe) {
            return new Bug(id);
        }
    }

    public List<String> getSavedQueries() {
        Object[] values = xmlRpcClient.executeCall("Search.get_all_saved_queries", getUsername());
        List<String> queries = new ArrayList<>();
        for (Object value : values) {
            queries.add(String.valueOf(value));
        }
        log.debug("Bugzilla queries for user {}, {}", getUsername(), queries.toString());
        return queries;
    }

    public boolean containsSavedQuery(String queryName) {
        List<String> savedQueries = getSavedQueries();
        return savedQueries.contains(queryName);
    }

    public void resolveBug(int bugId, BugResolutionType resolution) {
        Bug bugToResolve = getBugById(bugId);
        if (bugToResolve.resolution == resolution) {
            log.info("Bug with id {} already has resolution {}", bugId, resolution);
            return;
        }
        bugToResolve.knob = BugKnobType.resolve;
        bugToResolve.changed = 1;
        bugToResolve.resolution = resolution;
        SimpleDateFormat deltaDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZ");
        bugToResolve.delta_ts = deltaDateFormat.format(new Date());

        stripProductNameFromVersionIfPresent(bugToResolve);
        String response = connection.post(baseUrl + "process_bug.cgi", String.class, bugToResolve);

        Bug updatedBug = getBugById(bugId);
        if (updatedBug.resolution != resolution) {
            throw new InternalServerException(
                    format("Bug %s resolution was %s expected it to be %s after update\n%s", bugId, updatedBug.resolution, resolution, response));
        }
        log.info("Resolved bug {} with resolution {}", bugId, resolution.getValue());
    }

    public void addBugComment(int bugId, String comment) {
        SimpleDateFormat commentDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        xmlRpcClient.executeCall("Bug.add_comment", bugId, comment, commentDateFormat.format(new Date()), 1);
    }

    public String constructFullBugUrl(int bugNumber) {
        return baseUrl + "show_bug.cgi?id=" + bugNumber;
    }

    @Override
    public boolean isBaseUriTrusted() {
        return xmlRpcClient.isUriTrusted(URI.create(baseUrl));
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        getBugById(testBugNumber);
    }

    @Override
    protected void loginManually() {
        if (bugzillaSso) {
            Consumer<ChromeDevTools> ssoNavigateFunction = devTools -> {
            };

            Function<ChromeDevTools, String> apiTokenGenerator = devTools -> {
                return devTools.evaluate("document.cookie").getValue();
            };

            SsoClient ssoClient = new SsoClient(ssoConfig, getUsername(), "dbiggs@vmware.com");
            Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck = new AbstractMap.SimpleEntry<>(ApiRequest.evaluate("document.cookie"),
                    apiResponse -> apiResponse.getValue().contains(ApiAuthentication.bugzilla_cookie.getCookieName()));
            String cookies = ssoClient.loginAndGetApiToken(loggedInCheck, baseUrl, ssoButtonId, ssoNavigateFunction, apiTokenGenerator);
            String[] cookieTexts = cookies.split(";");
            Arrays.stream(cookieTexts).forEach(cookie -> connection.addCookie(new Cookie(URI.create(baseUrl).getHost(), cookie, "/")));
        } else {
            UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType, getUsername());
            try {
                Map result = xmlRpcClient.executeCall("User.login", credentials.toBugzillaLogin());
                Integer sessionId = (Integer) result.get("id");
                log.debug("Session id {}", sessionId);
            } catch (RuntimeXmlRpcException e) {
                if (e.getMessage() != null && e.getMessage().contains("The username or password you entered is not valid")) {
                    throw new NotAuthorizedException(e.getMessage(), e);
                } else {
                    throw e;
                }
            }
        }
    }

    private void stripProductNameFromVersionIfPresent(Bug bugToResolve) {
        String foundInVersionName = bugToResolve.foundInVersionName;
        if (bugToResolve.foundInProductName != null && foundInVersionName != null && foundInVersionName.startsWith(bugToResolve.foundInProductName)) {
            log.info("Stripping product name from version {}", foundInVersionName);
            bugToResolve.foundInVersionName = foundInVersionName.substring(bugToResolve.foundInProductName.length()).trim();
        }
    }

}

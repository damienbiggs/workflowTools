package com.vmware.jira;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.http.request.UrlParam;
import com.vmware.jira.domain.AccessToken;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolution;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTimeTracking;
import com.vmware.jira.domain.IssueTransition;
import com.vmware.jira.domain.IssueTransitions;
import com.vmware.jira.domain.IssueUpdate;
import com.vmware.jira.domain.IssuesResponse;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.MenuSection;
import com.vmware.jira.domain.MenuSections;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.trello.domain.StringValue;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.complexenum.ComplexEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vmware.http.cookie.ApiAuthentication.jira_token;
import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.New;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;
import static com.vmware.config.jira.IssueTypeDefinition.Bug;
import static com.vmware.config.jira.IssueTypeDefinition.Feature;
import static com.vmware.config.jira.IssueTypeDefinition.Improvement;
import static com.vmware.config.jira.IssueTypeDefinition.TechComm;
import static com.vmware.jira.domain.IssueStatusDefinition.WaitingForCodeReview;

public class Jira extends AbstractRestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String loginUrl;
    private final String searchUrl;
    private final String legacyApiUrl;
    private final String agileUrl;
    private final String greenhopperUrl;

    public Jira(String jiraUrl, String username, Map<String, String> customFieldNames) {
        super(jiraUrl, "rest/api/2/", ApiAuthentication.jira_token, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity, new ConfiguredGsonBuilder(customFieldNames).build());
        this.loginUrl = baseUrl + "login.jsp";
        this.searchUrl = apiUrl + "search";
        this.legacyApiUrl = baseUrl + "rest/api/1.0/";
        this.agileUrl = baseUrl + "rest/agile/1.0/";
        this.greenhopperUrl = baseUrl + "rest/greenhopper/1.0/";

        File accessTokenFile = new File(System.getProperty("user.home") + File.separator + ApiAuthentication.jira_token.getFileName());
        if (accessTokenFile.exists()) {
            log.debug("Using jira access token file {}", accessTokenFile.getAbsolutePath());
            connection.addStatefulParam(RequestHeader.aBearerAuthHeader(IOUtils.read(accessTokenFile)));
        }
    }

    public List<MenuItem> getRecentBoardItems() {
        List<MenuItem> recentItems = new ArrayList<MenuItem>();
        String url = legacyApiUrl + "menus/greenhopper_menu?inAdminMode=false";
        MenuSection[] sections = get(url, MenuSections.class).sections;
        if (sections.length == 0) {
            return recentItems;
        }

        for (MenuItem menuItem : sections[0].items) {
            if (menuItem.isRealItem()) {
                recentItems.add(menuItem);
            }
        }

        return recentItems;
    }

    public RapidView getRapidView(String viewId) {
        String url = greenhopperUrl + "xboard/plan/backlog/data.json";
        RapidView rapidView = get(url, RapidView.class, new UrlParam("rapidViewId", viewId));
        return rapidView;
    }

    public Issue getIssueByKey(String key) {
        return get(urlBaseForKey(key), Issue.class);
    }

    public Issue getIssueWithoutException(String key) {
        try {
            return getIssueByKey(key);
        } catch (NotFoundException e) {
            log.debug(e.getMessage(), e);
            return Issue.aNotFoundIssue(key);
        }
    }

    public IssuesResponse searchForIssues(SearchRequest searchRequest) {
        return post(searchUrl, IssuesResponse.class, searchRequest);
    }

    public IssuesResponse getOpenTasksForUser() {
        String allowedStatuses = generateNumericalEnumListAsInts(New, Open, Reopened, InProgress, InReview, WaitingForCodeReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND assignee=%s",
                issueTypesToGet, allowedStatuses, escapeUsername(getUsername()));
        IssuesResponse response = get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
        logInfoAboutResponse(response);

        return response;
    }

    public IssuesResponse getIssuesForUser(IssueStatusDefinition status, IssueResolutionDefinition resolution) {
        String jql = String.format("status=%s AND resolution=%s AND assignee=%s",
                status.getValue(), resolution != null ? resolution.getValue() : null, escapeUsername(getUsername()));
        IssuesResponse response = get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
        logInfoAboutResponse(response);
        return response;
    }

    public IssuesResponse getCreatedTasksForUser() {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND reporter in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(getUsername()));
        return get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
    }

    public IssueTransitions getAllowedTransitions(String key) {
        IssueTransitions transitions = get(urlBaseForKey(key) + "transitions", IssueTransitions.class);
        if (transitions == null) {
            transitions = new IssueTransitions();
        }
        transitions.issueKey = key;
        return transitions;
    }

    public void transitionIssue(IssueTransition transition) {
        transitionIssue(transition, null);
    }

    public void transitionIssue(IssueTransition transition, IssueResolutionDefinition resolution) {
        IssueUpdate updateIssue = new IssueUpdate(transition);
        if (resolution != null) {
            updateIssue.fields.resolution = new IssueResolution(resolution);
        }
        post(urlBaseForKey(transition.issueId) + "transitions", updateIssue);
    }

    public Issue createIssue(Issue issue) {
        return post(apiUrl + "issue", Issue.class, issue);
    }

    public void updateIssue(Issue issue) {
        connection.put(urlBaseForKey(issue.getKey()), issue);
    }

    public void updateIssueEstimate(String key, int estimateInHours) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.timetracking = new IssueTimeTracking(estimateInHours + "h");
        connection.put(urlBaseForKey(key), updateIssue);
    }

    public void updateIssueStoryPointsOnly(Issue issue) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.storyPoints = issue.fields.storyPoints;
        connection.put(urlBaseForKey(issue.getKey()), updateIssue);
    }

    public void updateIssueStoryPointsUsingAgileApi(Issue issue, String boardId) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.storyPoints = issue.fields.storyPoints;
        String url = agileUrl + "issue/" + issue.getKey() + "/estimation?boardId=" + boardId;
        StringValue storyPoints = new StringValue(String.valueOf(issue.fields.storyPoints));
        connection.put(url, storyPoints);
    }


    public void deleteIssue(String key) {
        connection.delete(urlBaseForKey(key));
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(jira_token, getUsername());
        connection.setupBasicAuthHeader(credentials);
        AccessToken token = connection.post(UrlUtils.addRelativePaths(baseUrl, "/rest/pat/latest/tokens"), AccessToken.class, new AccessToken("WorkflowTools"));
        connection.addStatefulParam(RequestHeader.aBearerAuthHeader(token.rawToken));
        saveApiToken(token.rawToken, jira_token);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        connection.get(baseUrl + "rest/auth/1/session",null);
    }

    public String urlBaseForKey(String key) {
        return apiUrl + "issue/" + key + "/";
    }

    private String generateNumericalEnumListAsInts(ComplexEnum... complexEnums) {
        return Arrays.stream(complexEnums).map(ComplexEnum::getValue).map(String::valueOf).collect(Collectors.joining(","));
    }

    private void logInfoAboutResponse(IssuesResponse response) {
        if (response == null) {
            log.debug("No issues parsed from jira response");
        } else {
            log.debug("{} tasks found", response.issues.length);
            if (response.total == 0 && response.warningMessages != null && response.warningMessages.length > 0) {
                log.warn("Failed to load tasks for user {}: {}", getUsername(), Arrays.toString(response.warningMessages));
            }
        }
    }

    private String escapeUsername(String username) {
        return username.replace(".", "\\\\u002e");
    }


}

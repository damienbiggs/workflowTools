package com.vmware.github;

import java.io.File;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.github.domain.GraphqlRequest;
import com.vmware.github.domain.MutationName;
import com.vmware.github.domain.PullMergeRequest;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.RequestedReviewers;
import com.vmware.github.domain.User;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.graphql.InputSerializer;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.ClasspathResource;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import static com.vmware.github.domain.MutationName.closePullRequest;
import static com.vmware.github.domain.MutationName.convertPullRequestToDraft;
import static com.vmware.github.domain.MutationName.createPullRequest;
import static com.vmware.github.domain.MutationName.markPullRequestReadyForReview;
import static com.vmware.github.domain.MutationName.mergePullRequest;
import static com.vmware.github.domain.MutationName.requestReviews;
import static com.vmware.github.domain.MutationName.updatePullRequest;

public class Github extends AbstractRestService {

    private final String graphqlUrl;
    private final Map<String, User> loginIdToUserOrTeamMap = new HashMap<>();
    private final InputSerializer inputSerializer = new InputSerializer();

    public Github(String baseUrl, String graphqlUrl) {
        super(baseUrl, "", ApiAuthentication.github_token, NULL_USERNAME);
        this.graphqlUrl = graphqlUrl;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
                        .namingStrategy(FieldNamingPolicy.IDENTITY)
                        .build());

        String apiToken = readExistingApiToken(ApiAuthentication.github_token);
        if (StringUtils.isNotBlank(apiToken)) {
            connection.addStatefulParam(RequestHeader.aBearerAuthHeader(apiToken));
        }
    }

    public boolean canConnect() {
        try {
            connection.get(baseUrl, String.class);
            return true;
        } catch (FatalException fe) {
            if (fe.getCause() instanceof UnknownHostException) {
                return false;
            }
            throw fe;
        }
    }

    public List<User> searchUsers(String companyName, String query) {
        GraphqlResponse response = postGraphql("/githubGraphql/searchUsers.txt",
                "query=" + query, "companyName=" + companyName);
        return response.data.search.usersForCompany(companyName);
    }

    public User getUserOrTeam(String orgName, String login) {
        if (loginIdToUserOrTeamMap.containsKey(login)) {
            return loginIdToUserOrTeamMap.get(login);
        }
        if (login.startsWith("@")) {
            GraphqlResponse response = postGraphql("/githubGraphql/team.txt", "login=" + login.substring(1), "orgName=" + orgName);
            if (response.data.organization.teams.nodes.length == 0) {
                throw new FatalException("Failed to find team named " + login.substring(1));
            }
            loginIdToUserOrTeamMap.put(login, response.data.organization.teams.nodes[0]);
            return response.data.organization.teams.nodes[0];
        } else {
            GraphqlResponse response = postGraphql("/githubGraphql/user.txt", "login=" + login);
            loginIdToUserOrTeamMap.put(login, response.data.user);
            return response.data.user;
        }
    }

    public PullRequest createPullRequest(PullRequest pullRequest) {
        GraphqlResponse repoResponse = postGraphql("/githubGraphql/repository.txt",
                "repoOwnerName=" + pullRequest.repoOwner(), "repoName=" + pullRequest.repoName());
        pullRequest.repositoryId = repoResponse.data.repository.id;

        String input = inputSerializer.serialize(pullRequest);
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        GraphqlResponse response = postGraphql("/githubGraphql/mutatePullRequest.txt", createPullRequest,
                "mutationName=" + createPullRequest, "input=" + input,
                "pullRequestResponse=" + pullRequestResponse);
        return response.data.mutatedPullRequest.pullRequest;
    }

    public void updatePullRequestBranch(PullRequest pullRequest) {
        PullMergeRequest updateRequest = new PullMergeRequest();
        updateRequest.pullRequestId = pullRequest.id;
        updateRequest.updateMethod = "REBASE";
        mutatePullRequest(pullRequest.id, updatePullRequest, updateRequest);
    }

    public Optional<PullRequest> getPullRequestForSourceBranch(String ownerName, String repoName, String sourceBranch, PullRequest.PullRequestState state) {
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        String stateParam = state != null ? "stateParam=states: " + state + ", " : "stateParam=";
        GraphqlResponse repository = postGraphql("/githubGraphql/pullRequests.txt",
                "repoOwnerName=" + ownerName, "repoName=" + repoName, stateParam, "headRef=" + sourceBranch, "pullRequestResponse=" + pullRequestResponse);
        return Arrays.stream(repository.data.repository.pullRequests.nodes).findFirst();
    }

    public PullRequest getPullRequest(String repoOwnerName, String repoName, long number) {
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        GraphqlResponse repository = postGraphql("/githubGraphql/pullRequest.txt",
                "repoOwnerName=" + repoOwnerName, "repoName=" + repoName, "pullRequestNumber=" + number, "pullRequestResponse=" + pullRequestResponse);
        return repository.data.repository.pullRequest;
    }

    public void mergePullRequest(PullRequest pullRequest, String mergeMethod) {
        PullMergeRequest pullMergeRequest = new PullMergeRequest();
        pullMergeRequest.pullRequestId = pullRequest.id;
        pullMergeRequest.mergeMethod = mergeMethod != null ? PullMergeRequest.MergeMethod.valueOf(mergeMethod.toUpperCase()) : null;
        pullMergeRequest.commitHeadline = pullRequest.title;
        pullMergeRequest.commitBody = pullRequest.body;
        pullMergeRequest.expectedHeadOid = pullRequest.headRefOid;

        PullRequest updatedPullRequest = mutatePullRequest(pullRequest.id, mergePullRequest, pullMergeRequest);
        log.debug("Merge result: {} Sha: {}", updatedPullRequest.state, updatedPullRequest.headRefOid);
        if (updatedPullRequest.state != PullRequest.PullRequestState.MERGED) {
            throw new FatalException("Failed to merge pull request {}", pullRequest.number);
        } else {
            log.info("Successfully merged pull request {}", pullRequest.number);
        }
    }

    public void updatePullRequestDetails(PullRequest pullRequest) {
        PullRequest pullRequestForUpdate = new PullRequest(pullRequest.id);
        pullRequestForUpdate.title = pullRequest.title;
        pullRequestForUpdate.body = pullRequest.body;
        mutatePullRequest(pullRequest.id, updatePullRequest, pullRequestForUpdate);
    }

    public void closePullRequest(PullRequest pullRequest) {
        PullRequest updatedPullRequest = mutatePullRequest(pullRequest, closePullRequest);
        if (updatedPullRequest.state != PullRequest.PullRequestState.CLOSED) {
            throw new FatalException("Failed to close pull request {}", pullRequest.number);
        }
    }

    public void markPullRequestAsDraft(PullRequest pullRequest) {
        log.info("Marking pull request {} as a draft", pullRequest.number);

        PullRequest updatedPullRequest = mutatePullRequest(pullRequest, convertPullRequestToDraft);
        if (!updatedPullRequest.isDraft) {
            throw new FatalException("Pull request {} draft status was not marked as a draft", updatedPullRequest.number);
        }
    }

    public void markPullRequestAsReadyForReview(PullRequest pullRequest) {
        log.info("Marking pull request {} as ready for review", pullRequest.number);

        PullRequest updatedPullRequest = mutatePullRequest(pullRequest, markPullRequestReadyForReview);
        if (updatedPullRequest.isDraft) {
            throw new FatalException("Pull request {} draft status was not marked as ready for review", updatedPullRequest.number);
        }
    }

    public void rerunFailedCheckRun(PullRequest pullRequest, String id) {
        post(UrlUtils.addRelativePaths(apiUrl, "repos", pullRequest.repoOwner(), pullRequest.repoName(), "actions/jobs", id, "rerun"), null);
    }

    public void updateReviewersForPullRequest(PullRequest pullRequest, List<String> usernames) {
        List<User> users = Arrays.stream(pullRequest.reviewRequests.nodes).filter(node -> !node.asCodeOwner)
                .map(node -> node.requestedReviewer).collect(Collectors.toList());
        boolean usersRemoved = users.removeIf(reviewer -> usernames.stream().noneMatch(username -> username.equals(reviewer.username())));
        List<User> usersToAdd = usernames.stream().filter(username -> users.stream().noneMatch(reviewer -> reviewer.username().equals(username)))
                .map(username -> getUserOrTeam(pullRequest.repoOwner(), username)).collect(Collectors.toList());
        if (usersRemoved || !usersToAdd.isEmpty()) {
            users.addAll(usersToAdd);
        }

        RequestedReviewers requestedReviewers = new RequestedReviewers(pullRequest.id, users);
        mutatePullRequest(pullRequest.id, requestReviews, requestedReviewers);
    }

    public ReleaseAsset[] getReleaseAssets(String releasePath) {
        return connection.get(UrlUtils.addRelativePaths(apiUrl, releasePath, "assets"), ReleaseAsset[].class);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        get(UrlUtils.addRelativePaths(apiUrl,"user"), String.class);
    }

    @Override
    protected void loginManually() {
        connection.removeStatefulParam(RequestHeader.AUTHORIZATION);
        log.info("Github uses personal access tokens for third party API access.");
        log.info("On the UI, go to Settings -> Developer Setting and create a new personal access token");
        String privateToken = InputUtils.readValueUntilNotBlank("Enter Personal Access Token");
        saveApiToken(privateToken, ApiAuthentication.github_token);
        connection.addStatefulParam(RequestHeader.aBearerAuthHeader(privateToken));
    }

    @Override
    protected File determineApiTokenFile(ApiAuthentication apiAuthentication) {
        String homeFolder = System.getProperty("user.home");
        File apiHostTokenFile = new File(homeFolder + File.separator +
                "." + URI.create(baseUrl).getHost() + "-" + apiAuthentication.getFileName().substring(1));
        if (apiHostTokenFile.exists()) {
            return apiHostTokenFile;
        } else {
            log.debug("Host api token file {} does not exist", apiHostTokenFile.getPath());
        }
        return super.determineApiTokenFile(apiAuthentication);
    }

    private PullRequest mutatePullRequest(PullRequest pullRequest, MutationName mutationName) {
        return mutatePullRequest(pullRequest.id, mutationName, new PullRequest(pullRequest.id));
    }

    private PullRequest mutatePullRequest(String pullRequestId, MutationName mutationName, Object inputObject) {
        List<String> paramList = new ArrayList<>();
        paramList.add("input=" + inputSerializer.serialize(inputObject));
        paramList.add("mutationName=" + mutationName.name());
        paramList.add("pullRequestResponse={id, number, headRefOid, isDraft, state}");
        String fileName = "/githubGraphql/mutatePullRequest.txt";

        GraphqlResponse response = postGraphql(fileName, mutationName, paramList.toArray(new String[0]));

        PullRequest updatedPullRequest = response.data.mutatedPullRequest.pullRequest;
        if (!Objects.equals(updatedPullRequest.id, pullRequestId)) {
            throw new FatalException("Wrong pull request {} was updated", updatedPullRequest.number);
        }
        return updatedPullRequest;
    }

    private GraphqlResponse postGraphql(String fileName, String... params) {
        return postGraphql(fileName, null, params);
    }

    private GraphqlResponse postGraphql(String fileName, MutationName mutationName, String... params) {
        Map<String, String> paramMap = Arrays.stream(params).map(param -> StringUtils.splitOnlyOnce(param, "="))
                .collect(Collectors.toMap(param -> param[0], param -> param[1]));
        String query = new ClasspathResource(fileName, this.getClass()).getText();
        GraphqlRequest request = new GraphqlRequest(query);
        paramMap.entrySet().stream().filter(entry -> entry.getValue() != null)
                .forEach(entry -> request.query = request.query.replace("${" + entry.getKey() + "}", entry.getValue()));
        log.trace("Graphql request: {}", request.query);

        String source = mutationName != null ? mutationName.name() : StringUtils.substringAfterLast(fileName, "/");
        GraphqlResponse response = post(graphqlUrl, GraphqlResponse.class, request, new RequestHeader("Source", source));
        if (response.errors != null && response.errors.length > 0) {
            log.debug("{}\nfailed with errors\n{} ", request.query, Arrays.toString(response.errors));
            throw new FatalException(Arrays.stream(response.errors).map(error -> error.message).collect(Collectors.joining(", ")));
        }
        return response;
    }
}

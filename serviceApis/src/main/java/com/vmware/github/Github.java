package com.vmware.github;

import java.io.File;
import java.net.URI;
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
    private final Map<String, User> loginIdToUserMap = new HashMap<>();
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

    public List<User> searchUsers(String companyName, String query) {
        GraphqlResponse response = postGraphql("/githubGraphql/searchUsers.txt",
                "query=" + query, "companyName=" + companyName);
        return response.data.search.usersForCompany(companyName);
    }

    public User getUser(String login) {
        if (loginIdToUserMap.containsKey(login)) {
            return loginIdToUserMap.get(login);
        }
        GraphqlResponse response = postGraphql("/githubGraphql/userDetails.txt", "loginId=" + login);
        loginIdToUserMap.put(login, response.data.user);
        return response.data.user;
    }

    public PullRequest createPullRequest(String repoOwnerName, String repoName, PullRequest pullRequest) {
        setupAuthenticatedConnection();
        GraphqlResponse repoResponse = postGraphql("/githubGraphql/repository.txt", "repoOwnerName=" + repoOwnerName, "repoName=" + repoName);
        pullRequest.repositoryId = repoResponse.data.repository.id;

        String input = inputSerializer.serialize(pullRequest);
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        GraphqlResponse response = postGraphql("/githubGraphql/createPullRequest.txt",
                "mutationName=" + createPullRequest, "input=" + input, "pullRequestResponse=" + pullRequestResponse);
        return response.data.mutatedPullRequest.pullRequest;
    }

    public void updatePullRequestBranch(PullRequest pullRequest) {
        PullMergeRequest updateRequest = new PullMergeRequest();
        updateRequest.pullRequestId = pullRequest.id;
        updateRequest.updateMethod = "REBASE";
        mutatePullRequest(pullRequest.id, updatePullRequest, updateRequest);
    }

    public Optional<PullRequest> getPullRequestForSourceBranch(String ownerName, String repoName, String sourceBranch) {
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        GraphqlResponse repository = postGraphql("/githubGraphql/pullRequests.txt",
                "repoOwnerName=" + ownerName, "repoName=" + repoName, "headRef=" + sourceBranch, "pullRequestResponse=" + pullRequestResponse);
        return Arrays.stream(repository.data.repository.pullRequests.nodes).findFirst();
    }

    public PullRequest getPullRequest(String repoOwnerName, String repoName, long number) {
        String pullRequestResponse = new ClasspathResource("/githubGraphql/fullPullRequestResponse.txt", this.getClass()).getText();
        GraphqlResponse repository = postGraphql("/githubGraphql/pullRequest.txt",
                "repoOwnerName=" + repoOwnerName, "repoName=" + repoName, "pullRequestNumber=" + number, "pullRequestResponse=" + pullRequestResponse);
        return repository.data.repository.pullRequest;
    }

    public void mergePullRequest(PullRequest pullRequest, String mergeMethod, String commitTitle, String commitMessage) {
        setupAuthenticatedConnection();
        PullMergeRequest pullMergeRequest = new PullMergeRequest();
        pullMergeRequest.pullRequestId = pullRequest.id;
        pullMergeRequest.mergeMethod = mergeMethod;
        pullMergeRequest.commitHeadline = commitTitle;
        pullMergeRequest.commitBody = commitMessage;
        pullMergeRequest.expectedHeadOid = pullRequest.headRefOid;
        PullRequest updatedPullRequest = mutatePullRequest(pullRequest.id, mergePullRequest, pullMergeRequest);
        log.debug("Merge result: {} Sha: {}", updatedPullRequest.merged, updatedPullRequest.headRefOid);
        if (!pullRequest.merged) {
            throw new FatalException("Failed to merge pull request {}", pullRequest.number);
        }
    }

    public void updatePullRequestDetails(PullRequest pullRequest) {
        setupAuthenticatedConnection();
        PullRequest pullRequestForUpdate = new PullRequest(pullRequest.id);
        pullRequestForUpdate.title = pullRequest.title;
        pullRequestForUpdate.body = pullRequest.body;
        mutatePullRequest(pullRequest.id, updatePullRequest, pullRequestForUpdate);
    }

    public void closePullRequest(PullRequest pullRequest) {
        PullRequest updatedPullRequest = mutatePullRequest(pullRequest, closePullRequest);
        if (!updatedPullRequest.closed) {
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

    public void updateReviewersForPullRequest(PullRequest pullRequest, List<User> users) {
        RequestedReviewers requestedReviewers = new RequestedReviewers(users);
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
        String input = inputSerializer.serialize(inputObject);

        List<String> paramList = new ArrayList<>();
        paramList.add("input=" + input);
        paramList.add("mutationName=" + mutationName.name());
        String fileName = "/githubGraphql/updatePullRequest.txt";

        GraphqlResponse response = postGraphql(fileName, paramList.toArray(new String[0]));

        PullRequest updatedPullRequest = response.data.mutatedPullRequest.pullRequest;
        if (!Objects.equals(updatedPullRequest.id, pullRequestId)) {
            throw new FatalException("Wrong pull request {} was updated", updatedPullRequest.number);
        }
        return updatedPullRequest;
    }

    private GraphqlResponse postGraphql(String fileName, String... params) {
        Map<String, String> paramMap = Arrays.stream(params).map(param -> StringUtils.splitOnlyOnce(param, "="))
                .collect(Collectors.toMap(param -> param[0], param -> param[1]));
        String query = new ClasspathResource(fileName, this.getClass()).getText();
        GraphqlRequest request = new GraphqlRequest(query);
        paramMap.forEach((key, value) -> request.query = request.query.replace("${" + key + "}", value));
        log.trace("Graphql request: {}", request.query);

        GraphqlResponse response = post(graphqlUrl, GraphqlResponse.class, request, new RequestHeader("Source", StringUtils.substringAfterLast(fileName, "/")));
        if (response.errors != null && response.errors.length > 0) {
            throw new FatalException("{}\nfailed with errors\n{} ", request.query, Arrays.toString(response.errors));
        }
        return response;
    }
}

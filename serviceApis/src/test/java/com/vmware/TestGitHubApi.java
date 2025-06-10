package com.vmware;

import com.google.gson.Gson;
import com.vmware.github.Github;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.MutationName;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestGitHubApi extends BaseTests{

    private Github github;

    @Before
    public void init() {
        String url = testProperties.getProperty("github.url");
        String graphql = testProperties.getProperty("github.graphql");
        github = new Github(url, graphql);
    }

    @Test
    public void responseDataIsDynamicallyConverted() {
        Gson gson = new Gson();
        Arrays.stream(MutationName.values()).forEach(name -> {
            String dummyResponse = "{\"data\": { \"" + name + "\":null}}";
            GraphqlResponse graphqlResponse = gson.fromJson(dummyResponse, GraphqlResponse.class);
            assertNull(graphqlResponse.data.mutatedPullRequest);
        });

        Arrays.stream(MutationName.values()).forEach(name -> {
            String dummyResponse = "{\"data\": {\"" + name + "\":{\"pullRequest\":{\"id\":\"test\"}}}}";
            GraphqlResponse graphqlResponse = gson.fromJson(dummyResponse, GraphqlResponse.class);
            assertEquals(graphqlResponse.data.mutatedPullRequest.pullRequest.id, "test");
        });
    }

    @Test
    public void getPullRequest() {
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 18);
        assertEquals(18, pullRequest.number);
    }

    @Test
    public void getReleaseAsset() {
        ReleaseAsset[] assets = github.getReleaseAssets("repos/vmware/workflowTools/releases/43387689");
        assertEquals("workflowTools.jar", assets[0].name);
    }

    @Test
    public void getReviewThreads() {
        github.setupAuthenticatedConnection();
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 12);
        assertTrue(pullRequest.reviewThreads.nodes.length > 0);
    }

    @Test
    public void searchUsers() {
        github.setupAuthenticatedConnection();
        List<User> users = github.searchUsers("jenkinsci", "damienbigg");
        assertEquals(1, users.size());
        assertEquals(users.get(0).name, "Damien Biggs");
    }
}

package com.vmware;

import com.vmware.github.Github;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.User;
import com.vmware.xmlrpc.MapObjectConverter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void testConversion() {
        Map<String, Object> response = new HashMap<>();
        response.put("number", 55D);
        response.put("isDraft", true);
        PullRequest pullRequest = new MapObjectConverter().fromMap(response, PullRequest.class);
        assertEquals(55d, pullRequest.number, 0d);
        assertTrue(pullRequest.isDraft);
    }

    @Test
    public void getPullRequest() {
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 18);
        assertEquals(18, pullRequest.number);
    }

    @Test
    public void getPullRequestNode() {
        PullRequest pullRequest = github.getPullRequest("vcf", "automation", 11702);
        assertEquals(11702, pullRequest.number);
    }

    @Test
    public void getReleaseAsset() throws IOException {
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
        List<User> users = github.searchUsers("VMware", "damienbigg");
        assertEquals(1, users.size());
    }
}

package com.vmware.github.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PullRequest {
    public enum PullRequestReviewDecision {
        APPROVED, CHANGES_REQUEST, REVIEW_REQUIRED
    }

    public PullRequestReviewDecision reviewDecision;
    public ReviewThreadNodes reviewThreads;
    @SerializedName("reviews")
    public ReviewNodes approvedReviews;

    public ReviewRequestNodes reviewRequests;
    public CommitsNode commits;

    @Expose(serialize = false)
    public String id;
    @Expose(deserialize = false)
    public String pullRequestId;
    public String repositoryId;
    public long number;

    @Expose(serialize = false)
    public Boolean isDraft;
    @Expose(deserialize = false)
    public Boolean draft;
    public boolean merged;
    public boolean closed;
    public String url;
    public String title;
    public String body;
    public String baseRefName;
    public String headRefName;
    public String headRefOid;
    @Expose(serialize = false)
    public HeadRepository headRepository;

    public PullRequest() {
    }

    public PullRequest(String pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    public String repoName() {
        return headRepository.name;
    }

    public String repoOwner() {
        return headRepository.owner.login;
    }

    public String reviewers() {
        if (reviewRequests == null || reviewRequests.nodes == null) {
            return "";
        }
        return Arrays.stream(reviewRequests.nodes).map(node -> node.requestedReviewer.username()).collect(Collectors.joining(","));
    }

    public Commit.Status checksStatus() {
        return commits.nodes[0].commit.statusCheckRollup.status;
    }

    public List<Commit.StatusNode> checks() {
        List<Commit.StatusNode> nodes = new ArrayList<>();
        Arrays.stream(commits.nodes[0].commit.statusCheckRollup.contexts.nodes).forEach(node -> {
            int existingNodeIndex = nodes.indexOf(node);
            if (existingNodeIndex == -1 || node.createdAt.after(nodes.get(existingNodeIndex).createdAt)) {
                if (existingNodeIndex != -1) {
                    nodes.remove(existingNodeIndex);
                }
                nodes.add(node);
            }
        });
        nodes.sort(Comparator.comparing(Commit.StatusNode::getCreatedAt));
        return nodes;
    }

    public List<User> approvers() {
        return Arrays.stream(approvedReviews.nodes).map(node -> node.author).collect(Collectors.toList());
    }

    public String asText() {
        return title + "\n" + body;
    }

    public static class CommitsNode {
        public CommitNode[] nodes;
    }

    public static class CommitNode {
        public Commit commit;
    }

    public static class ReviewNodes {
        public ReviewNode[] nodes;
    }

    public class ReviewNode {
        public User author;
    }

    public class ReviewRequestNodes {
        public ReviewRequestNode[] nodes;
    }

    public class ReviewRequestNode {
        public User requestedReviewer;
    }

    public static class HeadRepository {
        public HeadRepository() {
        }

        public HeadRepository(String owner, String name) {
            this.name = name;
            this.owner = new User(owner);
        }

        public String name;
        public User owner;
    }
}

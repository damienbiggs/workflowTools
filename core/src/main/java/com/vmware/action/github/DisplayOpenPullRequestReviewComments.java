package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReviewComment;
import com.vmware.github.domain.ReviewThread;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ActionDescription("Displays open diff and general comments for pull request review.")
public class DisplayOpenPullRequestReviewComments extends BaseCommitWithPullRequestAction {
    private final Map<String, String[]> diffFiles = new HashMap<>();

    public DisplayOpenPullRequestReviewComments(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.debug("Head sha for pull request {}", pullRequest.head.sha);
        GraphqlResponse.PullRequestNode pullRequestNode = github.getPullRequestViaGraphql(pullRequest);
        boolean firstThreadFound = false;
        for (ReviewThread reviewThread : pullRequestNode.reviewThreads.nodes) {
            if (reviewThread.isResolved) {
                continue;
            }

            if (!firstThreadFound) {
                log.info("Displaying open pull request review comments for {}", pullRequest.htmlUrl);
            }

            String path = reviewThread.path.substring(reviewThread.path.length() > 60 ? reviewThread.path.length() - 60 : 0);
            Padder threadPadder = new Padder(path);
            threadPadder.infoTitle();
            List<ReviewComment> comments = Arrays.asList(reviewThread.comments.nodes);
            comments.sort(Comparator.comparing(ReviewComment::getCreatedAt));
            if (!comments.isEmpty()) {
                log.info(comments.get(0).diffHunk);
                log.info("");
            }
            for (ReviewComment comment : comments) {
                String authorInfo = comment.author.login + " - ";
                String body = comment.body.replace("\n", "\n" + StringUtils.repeat(authorInfo.length(), " "));
                log.info("{}{}", authorInfo, body);
            }
            threadPadder.infoTitle();
        }

        if (!firstThreadFound) {
            log.info("No open pull request review comments for {}", pullRequest.htmlUrl);
        }
    }

}

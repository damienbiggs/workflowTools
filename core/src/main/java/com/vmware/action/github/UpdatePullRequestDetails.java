package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.User;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Updates the title, description and reviewers for a pull request")
public class UpdatePullRequestDetails extends BaseCommitWithPullRequestAction {
    public UpdatePullRequestDetails(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.info("Updating details for pull request {}", pullRequest.url);
        pullRequest.title = draft.summary;
        pullRequest.body = draft.toText(commitConfig, false, false);
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as pull request is already associated with a reviewboard review");
        } else if (draft.hasReviewers()) {
            List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
            List<User> existingReviewers = Arrays.stream(pullRequest.reviewRequests.nodes).map(node -> node.requestedReviewer).collect(Collectors.toList());
            boolean usersRemoved = existingReviewers.removeIf(reviewer -> usernames.stream().noneMatch(username -> username.equals(reviewer.username())));
            List<User> usersToAdd = usernames.stream().filter(username -> existingReviewers.stream().noneMatch(reviewer -> reviewer.username().equals(username)))
                    .map(username -> github.getUser(username)).collect(Collectors.toList());
            if (usersRemoved || !usersToAdd.isEmpty()) {
                existingReviewers.addAll(usersToAdd);
            }
            github.updateReviewersForPullRequest(pullRequest, existingReviewers);
        }
        github.updatePullRequestDetails(pullRequest);
    }
}

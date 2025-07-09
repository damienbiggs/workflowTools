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
        log.info("Updating details for pull request {}", pullRequest.number);
        pullRequest.title = draft.summary;
        pullRequest.body = draft.toText(commitConfig, false, false, false);
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as pull request is already associated with a reviewboard review");
        } else if (draft.hasReviewers()) {
            List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
            github.updateReviewersForPullRequest(pullRequest, usernames);
        }
        github.updatePullRequestDetails(pullRequest);
    }
}

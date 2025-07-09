package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.User;
import com.vmware.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.github.domain.PullRequest.PullRequestReviewDecision.REVIEW_REQUIRED;

@ActionDescription("Exists if the pull request does not have all required approvals")
public class ExitIfPullRequestDoesNotHaveRequiredApprovals extends BaseCommitWithPullRequestAction {
    public ExitIfPullRequestDoesNotHaveRequiredApprovals(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "reviewboard request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(draft.reviewedBy) || commitConfig.trivialReviewerLabel.equals(draft.reviewedBy)) {
            return;
        }
        PullRequest pullRequest = draft.getGithubPullRequest();
        List<User> approvers = pullRequest.approvers();
        if (approvers.isEmpty()) {
            cancelWithMessage("no approved reviews found for pull request {}", pullRequest.number);
        } else {
            draft.shipItReviewers = approvers.stream().map(user -> user.name).collect(Collectors.joining(", "));
            if (pullRequest.reviewDecision == REVIEW_REQUIRED) {
                cancelWithMessage("still need more approvals, already approved by {}", draft.shipItReviewers);
            }
            log.info("Approved by {}", draft.shipItReviewers);
        }
    }
}

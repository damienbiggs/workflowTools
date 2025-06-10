package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Sets the reviewer list for the commit as the list of users who have approved the associated pull request.")
public class SetReviewedByAsApprovedReviewersList extends BaseCommitWithPullRequestAction {
    public SetReviewedByAsApprovedReviewersList(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "reviewboard request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        // reuse the result from other actions like ExitIfPullRequestDoesNotHaveRequiredApprovals
        if (draft.shipItReviewers != null) {
            draft.reviewedBy = draft.shipItReviewers;
            draft.shipItReviewers = null;
            return;
        }

        List<String> approvers = draft.getGithubPullRequest().approvers();
        if (approvers.isEmpty()) {
            log.info("No approved reviews found for pull request");
            return;
        }

        draft.reviewedBy = StringUtils.join(approvers);
    }
}

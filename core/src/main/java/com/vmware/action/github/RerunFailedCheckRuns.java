package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.Commit;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Rerun the failed check runs for a pull request.")
public class RerunFailedCheckRuns extends BaseCommitWithPullRequestAction {

    public RerunFailedCheckRuns(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        List<Commit.StatusNode> checks = pullRequest.checks();
        List<Commit.StatusNode> failedChecks = checks.stream().filter(check -> StringUtils.isNotBlank(check.databaseId)
                && check.status == Commit.Status.FAILURE).collect(Collectors.toList());
        if (failedChecks.isEmpty()) {
            log.info("No failed checks found for pull request {}", pullRequest.number);
            return;
        }

        log.info("Rerunning {} for pull request {}", StringUtils.pluralize(failedChecks.size(), "failed check"), pullRequest.number);
        failedChecks.forEach(check -> {
            log.info("Rerunning check {}", check.fullName());
            github.rerunFailedCheckRun(pullRequest, check.databaseId);
        });
    }
}

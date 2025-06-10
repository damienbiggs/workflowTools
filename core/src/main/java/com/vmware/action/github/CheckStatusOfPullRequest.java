package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.Commit;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Checks the status of review approvals and checks for a pull request")
public class CheckStatusOfPullRequest extends BaseCommitWithPullRequestAction {
    public CheckStatusOfPullRequest(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();

        Padder statusPadder = new Padder("Pull Request {} Status", pullRequest.number);
        statusPadder.infoTitle();
        if (pullRequest.isDraft) {
            log.info("Pull Request is marked as draft!");
        }
        log.debug("Pull request checks status: {}", pullRequest.checksStatus());
        List<Commit.StatusNode> nodes = pullRequest.checks();
        DynamicLogger logger = new DynamicLogger(log);
        nodes.forEach(node -> {
            String targetUrl = node.targetUrl != null ? "(" + node.targetUrl + ")" : "";
            String description = node.summary != null ? node.summary :
                    node.status != null ? node.status.name() : Commit.Status.PENDING.name();
            LogLevel level = node.status == Commit.Status.SUCCESS ? LogLevel.DEBUG : LogLevel.INFO;
            logger.log(level, "{}{} - {}", node.fullName(), targetUrl, description);
        });

        log.info("Pull request approval status: {}", pullRequest.reviewDecision);
        List<String> approverIds = pullRequest.approvers();
        String approverNames = approverIds.stream().map(id -> github.getUser(id).name).collect(Collectors.joining(", "));
        if (approverIds.isEmpty()) {
            log.info("Not approved by any reviewers yet");
        } else {
            log.info("Pull request {} approved by {}", pullRequest.number, approverNames);
        }
        statusPadder.infoTitle();
    }
}

package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.Commit;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.User;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.util.StringUtils.isNotBlank;

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
        DynamicLogger logger = new DynamicLogger(log);

        String labels = pullRequest.labels();
        logger.log(StringUtils.isNotBlank(labels) ? LogLevel.INFO : LogLevel.DEBUG, "Labels: {}", pullRequest.labels());
        log.debug("Checks status: {}", pullRequest.checksStatus());
        List<Commit.StatusNode> nodes = pullRequest.checks();
        nodes.forEach(node -> {
            String targetUrl = isNotBlank(node.targetUrl) ? "(" + node.targetUrl + ")" : "";
            String description = isNotBlank(node.summary) ? node.summary :
                    node.status != null ? node.status.name() : Commit.Status.PENDING.name();
            LogLevel level = node.status == Commit.Status.SUCCESS ? LogLevel.DEBUG : LogLevel.INFO;
            logger.log(level, "{}{} - {}", node.fullName(), targetUrl, description);
        });

        logger.log(pullRequest.state == PullRequest.PullRequestState.OPEN ? LogLevel.DEBUG : LogLevel.INFO,
                "State: {}", pullRequest.state);
        log.info("Checks summary: {}", pullRequest.checksSummary());
        log.info("Approval status: {}", pullRequest.reviewDecision);

        List<User> approverIds = pullRequest.approvers();
        String approverNames = approverIds.stream().map(user -> user.name).collect(Collectors.joining(", "));
        if (approverIds.isEmpty()) {
            log.info("Not approved by any reviewers yet");
        } else {
            log.info("Approved by {}", approverNames);
        }
        statusPadder.infoTitle();
    }
}

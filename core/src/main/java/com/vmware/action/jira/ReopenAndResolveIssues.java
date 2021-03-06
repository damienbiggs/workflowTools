package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTransition;
import com.vmware.jira.domain.IssueTransitions;

import static com.vmware.jira.domain.IssueResolutionDefinition.Fixed;
import static com.vmware.jira.domain.IssueStatusDefinition.Closed;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;
import static com.vmware.jira.domain.IssueStatusDefinition.Resolved;

@ActionDescription("Reopens specified issues that have no resolution and resolve them.")
public class ReopenAndResolveIssues extends BaseBatchJiraAction{
    public ReopenAndResolveIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (multiActionData.noIssuesAdded()) {
            return "no issues added";
        }

        return super.cannotRunAction();
    }

    @Override
    public void process() {
        int processingCounter = 0;
        for (Issue issueToReopen : multiActionData.getIssuesFromJira()) {
            IssueTransition reopenTransition = getIssueTransition(issueToReopen, Reopened);
            if (reopenTransition == null) {
                continue;
            }

            log.info("Reopening issue {} that has no resolution", issueToReopen.getKey());
            jira.transitionIssue(reopenTransition);

            IssueTransition resolveTransition = getIssueTransition(issueToReopen, Resolved);
            if (resolveTransition == null) {
                continue;
            }

            log.info("Resolving issue {} with a resolution of {}", issueToReopen.getKey(), Fixed.name());
            jira.transitionIssue(resolveTransition);
            processingCounter++;
        }
        log.info("Successfully reopened and resolved {} issues", processingCounter);
    }

    private IssueTransition getIssueTransition(Issue issueToReopen, IssueStatusDefinition transitionForStatus) {
        if (issueToReopen.getStatus() != Closed) {
            log.info("Issue {} has a status of {}, skipping", issueToReopen.getKey(), issueToReopen.getStatus().name());
            return null;
        }

        if (issueToReopen.getResolution() != null) {
            log.info("Issue {} already has a resolution of {}, skipping", issueToReopen.getKey(), issueToReopen.getResolution().name());
            return null;
        }

        IssueTransitions transitions = jira.getAllowedTransitions(issueToReopen.getKey());
        if (!transitions.canTransitionTo(transitionForStatus)) {
            log.info("Issue {} cannot be transitioned to {}", issueToReopen.getKey(), transitionForStatus.name());
            return null;
        }

        return transitions.getTransitionForStatus(transitionForStatus);
    }
}

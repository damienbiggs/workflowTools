package com.vmware.action.jira;

import com.vmware.action.base.BaseTransitionJiraIssue;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.IssueStatusDefinition;

import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;

@ActionDescription("Marks the jira issue identified by the bug number as in progress if it has a status of open or reopened.")
public class MarkIssueAsInProgress extends BaseTransitionJiraIssue {

    public MarkIssueAsInProgress(WorkflowConfig config) {
        super(config, new IssueStatusDefinition[] {InProgress}, new IssueStatusDefinition[] {Open, Reopened});
    }
}

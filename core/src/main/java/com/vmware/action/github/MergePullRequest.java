package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

@ActionDescription("Merge pull request in Github so that it is merged to the target branch.")
public class MergePullRequest extends BaseCommitWithPullRequestAction {
    public MergePullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.info("Merging pull request {}", draft.requestUrl);
        github.mergePullRequest(pullRequest, githubConfig.mergeMethod);
    }
}

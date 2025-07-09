package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Performs a git push origin HEAD:topic/[username config property]/pre-commit.")
public class PushToPrecommitBranch extends BaseCommitAction {

    public PushToPrecommitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranchName = "precommit";
        String remoteBranchPath = gitRepoConfig.remoteBranches.get(remoteBranchName);
        if (StringUtils.isEmpty(remoteBranchPath)) {
            remoteBranchPath = "topic/$USERNAME/pre-commit";
        }

        String username = serviceLocator.determineUsername(gitRepoConfig.gitRemoteBranchUsername);
        remoteBranchPath = remoteBranchPath.replace("$USERNAME", username);

        git.pushToRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranchPath, gitRepoConfig.forcePush);
    }
}

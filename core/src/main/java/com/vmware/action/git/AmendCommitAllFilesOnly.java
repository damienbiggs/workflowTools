package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Performs a git commit --amend --all without modifying any part of the commit message. Uses the existing commit message.")
public class AmendCommitAllFilesOnly extends BaseCommitAmendAction {

    public AmendCommitAllFilesOnly(WorkflowConfig config) {
        super(config, true);
    }

    @Override // always run
    public void checkIfActionShouldBeSkipped() {
    }

    @Override
    public void process() {
        String description = draft.toText(commitConfig, commitConfig.includeJobResults);
        if (git.workingDirectoryIsInGitRepo()) {
            commitUsingGit(description);
        } else if (StringUtils.isNotEmpty(perforceClientConfig.perforceClientName)) {
            commitUsingPerforce(description);
        }
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommitAll(git.lastCommitBody(), gitRepoConfig.noVerify);
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, LogLevel.INFO);
    }

}

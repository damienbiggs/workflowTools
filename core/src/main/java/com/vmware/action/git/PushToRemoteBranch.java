package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:[remote branch path] -f.")
public class PushToRemoteBranch extends BaseAction {

    public PushToRemoteBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranchName = config.remoteBranchToUse;
        String remoteBranchPath = config.remoteBranches.get(remoteBranchName);
        if (StringUtils.isBlank(remoteBranchPath)) {
            log.info("{} did not match any predefined remote branch names {}.", remoteBranchName, config.remoteBranches.keySet().toString());
            log.info("Assuming that it is a valid remote branch path.");
            remoteBranchPath = remoteBranchName;
        }

        remoteBranchPath = remoteBranchPath.replace(":username", config.username);
        log.info("Updating remote branch " + remoteBranchPath);

        git.forcePushToRemoteBranch(remoteBranchPath);
    }
}

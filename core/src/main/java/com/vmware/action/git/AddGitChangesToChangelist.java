package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Runs git p4 --prepare-p4-only, then moves changes to the specified changelist.")
public class AddGitChangesToChangelist extends BaseCommitAction {
    public AddGitChangesToChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Using git p4 to add commit changes to default changelist in perforce");
        git.addChangesToDefaultChangelist();

        if (draft.perforceChangelistId == null) {
            log.warn("No changelist associated with commit, leaving changes in default changelist");
            return;
        }

        log.info("Moving changes to changelist {}", draft.perforceChangelistId);
        perforce.moveAllOpenFilesToChangelist(draft.perforceChangelistId);
    }
}

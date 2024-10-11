package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseSetUsersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.Collections;

@ActionDescription("Set the approved by users for a commit.")
public class SetApprovedBy extends BaseSetUsersList {
    public SetApprovedBy(WorkflowConfig config) {
        super(config, "approvedBy", CandidateSearchType.gitlab, false);
    }

    @Override
    public void process() {
        log.info("Enter users to approve commit");
        draft.approvedBy = readUsers(Collections.<String>emptySet(), draft.approvedBy, "Users (blank means no users)");
        log.info("Approved by user list: {}", draft.approvedBy);
    }
}

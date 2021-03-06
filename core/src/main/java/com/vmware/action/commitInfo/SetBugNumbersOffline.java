package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitReadAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

import java.io.IOException;

import static com.vmware.util.StringUtils.appendCsvValue;

@ActionDescription("Sets the bug number without showing assigned issues in Jira.")
public class SetBugNumbersOffline extends BaseCommitReadAction {

    public SetBugNumbersOffline(WorkflowConfig config) {
        super(config, "bugNumbers");
    }

    @Override
    public void process() {
        if (StringUtils.isNotBlank(draft.bugNumbers) && !draft.bugNumbers.equals(config.noBugNumberLabel)) {
            log.info("");
            log.info("Existing Bug Numbers: {}", draft.bugNumbers);
        }

        String[] bugNumbers = InputUtils.readData("Bug Numbers: (leave blank if no bug number)", true, 20).split(",");

        String bugNumberText = "";

        for (String bugNumber : bugNumbers) {
            bugNumberText = appendCsvValue(bugNumberText, bugNumber);
        }
        if (bugNumberText.isEmpty()) {
            bugNumberText = config.noBugNumberLabel;
        }
        draft.bugNumbers = bugNumberText;
        log.info("Bug number for commit: {}", draft.bugNumbers);
    }

}

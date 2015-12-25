package com.vmware.action.conditional;

import com.vmware.action.base.AbstractBatchIssuesAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Helper action for exiting if there are no project issues to process.")
public class ExitIfThereAreNoIssuesToProcess extends AbstractBatchIssuesAction {

    public ExitIfThereAreNoIssuesToProcess(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (projectIssues.isEmpty()) {
            System.exit(0);
        }
    }
}
package com.vmware.action.info;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

@ActionDescription("Displays all other workflows not categorized as main workflows.")
public class DisplayAdditionalWorkflows extends BaseAction {

    public DisplayAdditionalWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder additionalWorkflowsPadder = new Padder("Additional Workflows");
        additionalWorkflowsPadder.infoTitle();
        Set<String> sortedWorkflows = new TreeSet<String>(config.workflows.keySet());
        for (String workflow : sortedWorkflows) {
            if (!Workflow.MAIN_WORKFLOWS.contains(workflow) && !config.supportingWorkflows.contains(workflow)) {
                log.info("{} -> {}", workflow, Arrays.toString(config.workflows.get(workflow)));
            }
        }
        additionalWorkflowsPadder.infoTitle();
    }
}

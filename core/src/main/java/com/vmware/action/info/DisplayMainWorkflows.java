package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

import java.util.Map;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends BaseAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        config.mainWorkflowHelpMessages.entrySet().forEach(this::printWorkflows);
    }

    protected void printWorkflows(Map.Entry<String, Map<String, String>> workflows) {
        Padder mainWorkflowsPadder = new Padder(workflows.getKey() + " Workflows");
        mainWorkflowsPadder.infoTitle();
        for (Map.Entry<String, String> entry : workflows.getValue().entrySet()) {
            log.info("{} - {}", entry.getKey(), entry.getValue());
        }
        mainWorkflowsPadder.infoTitle();
    }
}

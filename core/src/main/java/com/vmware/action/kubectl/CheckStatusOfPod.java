package com.vmware.action.kubectl;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.commandline.Kubectl;
import com.vmware.util.logging.Padder;

import java.util.List;

@ActionDescription("Gets the status for a kubernetes pod")
public class CheckStatusOfPod extends BaseAction {
    public CheckStatusOfPod(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("kubectl");
        super.addFailWorkflowIfBlankProperties("kubeConfigFile", "podName");
    }

    @Override
    public void process() {
        Kubectl kubectl = new Kubectl(kubectlConfig.kubeConfigFile, kubectlConfig.namespace);
        List<String> tenantManagerStatus = kubectl.getPodStatus(kubectlConfig.podName);
        if (tenantManagerStatus.isEmpty()) {
            log.info("No pods found containing name {}", kubectlConfig.podName);
        } else {
            Padder podsPadder = new Padder(100, "{} Pods", kubectlConfig.podName);
            podsPadder.infoTitle();
            tenantManagerStatus.forEach(log::info);
            podsPadder.infoTitle();
        }
    }
}

package com.vmware.action.kubectl;

import com.vmware.action.base.BaseCommitUsingKubectlAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Tail log file from a pod")
public class TailPodLogFile extends BaseCommitUsingKubectlAction {
    public TailPodLogFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("logFile");
    }

    @Override
    public void process() {
        log.info("Tailing log file {}", kubectlConfig.logFile);
        String podName = kubectlConfig.podName;
        String suffix = StringUtils.substringAfterLast(podName, "-");
        if (!StringUtils.isInteger(suffix)) {
            log.debug("Appending suffix -0 to pod name {}", podName);
            podName += "-0";
        }
        kubectl.tailLogFile(podName, kubectlConfig.logFile, kubectlConfig.logLineCount, kubectlConfig.continuousTailing);
    }
}

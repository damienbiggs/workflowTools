package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.commandline.Kubectl;

public abstract class BaseCommitUsingKubectlAction extends BaseCommitAction{
    protected Kubectl kubectl;

    public BaseCommitUsingKubectlAction(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("kubectl");
        super.addFailWorkflowIfBlankProperties("kubeConfigFile", "namespace", "podName");
    }

    @Override
    public void preprocess() {
        super.preprocess();
        kubectl = new Kubectl(kubectlConfig.kubeConfigFile, kubectlConfig.namespace);
    }
}

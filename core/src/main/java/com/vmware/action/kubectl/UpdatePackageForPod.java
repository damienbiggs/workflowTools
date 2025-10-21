package com.vmware.action.kubectl;

import com.vmware.BuildStatus;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.buildweb.Buildweb;
import com.vmware.buildweb.domain.BuildwebBuild;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.kubectl.Kubectl;

@ActionDescription("Upgrade a pod in a vcfa instance using kubectl and vmsp")
public class UpdatePackageForPod extends BaseCommitAction {
    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";

    public UpdatePackageForPod(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("vmsp", "kubectl");
        super.addFailWorkflowIfBlankProperties("kubeConfigFile", "registryUrl", "podName");
    }

    @Override
    public void process() {
        Kubectl kubectl = new Kubectl(kubectlConfig.kubeConfigFile, kubectlConfig.namespace);

        String sandboxBuildNumber = StringUtils.isNotBlank(buildwebConfig.sandboxBuildNumber)
                ? buildwebConfig.sandboxBuildNumber : determineSandboxBuildNumber(buildwebConfig.buildDisplayName);
        Buildweb buildweb = serviceLocator.getBuildweb();
        BuildwebBuild sandbox = buildweb.getSandboxBuild(sandboxBuildNumber);
        if (sandbox.buildStatus != BuildStatus.SUCCESS) {
            throw new FatalException("Build {} has a status of {}", sandboxBuildNumber, sandbox.buildStatus);
        }
        String buildNumber = StringUtils.substringAfter(sandboxBuildNumber, "-");
        String packageUpdatePath = kubectlConfig.packageUpdatePath.replace(SANDBOX_BUILD_NUMBER, buildNumber);

        String downloadUrl = UrlUtils.addRelativePaths(sandbox.buildTreeUrl, packageUpdatePath);
        if (!UrlUtils.isUrlReachable(downloadUrl)) {
            throw new FatalException("Pkg url {} is not reachable", downloadUrl);
        }
        log.info("Kubeconfig file: {}", kubectlConfig.kubeConfigFile);
        log.info("Package update url: {}", downloadUrl);
        String answer = InputUtils.readValueUntilNotBlank(String.format("%s pod will be updated using sandbox build %s, continue (Y/N)",
                kubectlConfig.podName, sandboxBuildNumber));
        if (!answer.equalsIgnoreCase("Y")) {
            System.exit(0);
        }

        kubectl.pushPackageToRegistry(kubectlConfig.registryUrl, downloadUrl);
        kubectl.updatePackageVersionForPod(kubectlConfig.podName, sandboxBuildNumber);
        log.info("{} could take a few minutes to fully update", kubectlConfig.podName);
    }
}
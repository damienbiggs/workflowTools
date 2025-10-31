package com.vmware.action.kubectl;

import com.google.gson.Gson;
import com.vmware.BuildStatus;
import com.vmware.action.base.BaseCommitUsingKubectlAction;
import com.vmware.buildweb.Buildweb;
import com.vmware.buildweb.domain.BuildwebBuild;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.xmlrpc.MapObjectConverter;

import java.io.StringReader;
import java.util.Map;

@ActionDescription("Upgrade a pod in a vcfa instance using kubectl and vmsp")
public class UpdatePackageForPod extends BaseCommitUsingKubectlAction {
    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";

    public UpdatePackageForPod(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("vmsp");
        super.addFailWorkflowIfBlankProperties("registryUrl");
    }

    @Override
    public void process() {
        String sandboxBuildNumber = StringUtils.isNotBlank(buildwebConfig.sandboxBuildNumber)
                ? buildwebConfig.sandboxBuildNumber : determineSandboxBuildNumber(buildwebConfig.buildDisplayName);
        Buildweb buildweb = serviceLocator.getBuildweb();
        BuildwebBuild sandbox = buildweb.getSandboxBuild(sandboxBuildNumber);
        if (sandbox.buildStatus != BuildStatus.SUCCESS) {
            throw new FatalException("Build {}-{} has a status of {}", sandbox.buildSystem, sandbox.id, sandbox.buildStatus);
        }

        String packageUpdatePath = kubectlConfig.packageUpdatePath.replace(SANDBOX_BUILD_NUMBER, String.valueOf(sandbox.id));
        String downloadUrl = UrlUtils.addRelativePaths(sandbox.buildTreeUrl, packageUpdatePath);
        if (!UrlUtils.isUrlReachable(downloadUrl)) {
            throw new FatalException("Pkg url {} is not reachable", downloadUrl);
        }

        String podName = kubectlConfig.podName;
        Gson gson = new ConfiguredGsonBuilder().build();
        Map deployment = gson.fromJson(new StringReader(kubectl.getDeploymentInfo()), Map.class);
        Map spec = new MapObjectConverter().fromMap(deployment, Map.class,
                String.format("spec.packages[name=%s]", podName), 0, true);
        String currentVersion = (String) spec.get("version");
        String currentVersionNumber = StringUtils.substringAfterLast(currentVersion, ".");
        if (String.valueOf(sandbox.id).equals(currentVersionNumber)) {
            throw new FatalException("{} pod current version {} is the same as sandbox build number {}",
                    podName, currentVersionNumber, sandbox.id);
        }
        log.info("Kubeconfig file: {}", kubectlConfig.kubeConfigFile);
        log.info("Package update url: {}", downloadUrl);
        String answer = InputUtils.readValueUntilNotBlank(String.format("%s pod will be updated from %s using sandbox build %s-%s, continue (Y/N)",
                podName, currentVersion, sandbox.buildSystem, sandbox.id));
        if (!answer.equalsIgnoreCase("Y")) {
            System.exit(0);
        }

        kubectl.pushPackageToRegistry(kubectlConfig.registryUrl, downloadUrl);

        String versionPrefix = StringUtils.substringBeforeLast(currentVersion, ".");
        spec.put("version", versionPrefix + "." + sandbox.id);
        String updatedSpec = gson.toJson(deployment);
        kubectl.apply(updatedSpec);
        log.info("{} could take a few minutes to fully update", podName);
    }
}
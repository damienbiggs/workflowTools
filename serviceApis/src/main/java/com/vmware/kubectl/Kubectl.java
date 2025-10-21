package com.vmware.kubectl;

import com.google.gson.Gson;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;
import com.vmware.xmlrpc.MapObjectConverter;

import java.io.File;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.vmware.util.CommandLineUtils.executeCommand;

/**
 * Wrapper for Kubectl command line calls
 */
public class Kubectl {
    private final Map<String, String> envvars;
    private final String namespace;

    public Kubectl(String kubeConfigFile, String namespace) {
        this.namespace = namespace;
        if (!new File(kubeConfigFile).exists()) {
            throw new FatalException("kubeConfigFile {} does not exist", kubeConfigFile);
        }
        this.envvars = Collections.singletonMap("KUBECONFIG", kubeConfigFile);
    }

    public void pushPackageToRegistry(String registryUrl, String pkgUrl) {
        String namespaceSuffix = StringUtils.isNotBlank(namespace) ? "--namespace=" + namespace + " " : "";
        executeCommand(envvars, String.format("vmsp pkg push --remote %s %s %s ",namespaceSuffix, registryUrl, pkgUrl),
                null, LogLevel.INFO);
    }

    public void updatePackageVersionForPod(String packageName, String sandboxBuildNumber) {
        kubectl("wait bundle/tenant-manager --for=condition=Ready --timeout=5m", null, LogLevel.INFO);
        String deploymentInfo = kubectl("get packagedeployment/vcfa-bundle -o json", null, LogLevel.DEBUG);
        String updatedSpec = updateDeploymentPackage(deploymentInfo, packageName, sandboxBuildNumber);
        kubectl("apply -f -", updatedSpec, LogLevel.INFO);
    }

    public List<String> getPodStatus(String podName) {
        String output = kubectl("get pods", null, LogLevel.DEBUG);
        return MatcherUtils.allMatches(output, "(" + podName + ".+?)\n");
    }

    private String kubectl(String command, String inputText, LogLevel logLevel) {
        String namespaceSuffix = StringUtils.isNotBlank(namespace) ? "--namespace=" + namespace + " " : "";
        return executeCommand(envvars, "kubectl " + namespaceSuffix + command, inputText, logLevel);
    }

    private String updateDeploymentPackage(String deploymentInfo, String packageName, String sandboxBuildNumber) {
        Gson gson = new ConfiguredGsonBuilder().build();
        Map deployment = gson.fromJson(new StringReader(deploymentInfo), Map.class);
        Map spec = new MapObjectConverter().fromMap(deployment, Map.class, String.format("spec.packages[name=%s]", packageName), 0, true);
        String currentVersion = (String) spec.get("version");
        String versionPrefix = StringUtils.substringBeforeLast(currentVersion, ".");
        spec.put("version", versionPrefix + "." + sandboxBuildNumber);
        String updatedSpec = gson.toJson(deployment);
        return updatedSpec;
    }
}

package com.vmware.util.commandline;

import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for Kubectl command line calls
 */
public class Kubectl extends BaseCommandLineClient {
    private final Map<String, String> envvars;
    private final String namespace;

    public Kubectl(String kubeConfigFile, String namespace) {
        super(CommandLineClientType.k8s);
        super.setWorkingDirectory(System.getProperty("user.dir"));
        this.namespace = namespace;
        if (!new File(kubeConfigFile).exists()) {
            throw new FatalException("kubeConfigFile {} does not exist", kubeConfigFile);
        }
        this.envvars = Collections.singletonMap("KUBECONFIG", kubeConfigFile);
    }

    public void pushPackageToRegistry(String registryUrl, String pkgUrl) {
        String namespaceSuffix = StringUtils.isNotBlank(namespace) ? "--namespace=" + namespace + " " : "";
        CommandLineUtils.executeCommand(envvars,
                String.format("vmsp pkg push --remote %s %s %s ", namespaceSuffix, registryUrl, pkgUrl),
                null, LogLevel.INFO);
        execute(envvars, "wait bundle/tenant-manager --for=condition=Ready --timeout=5m", LogLevel.INFO);
    }

    public String getDeploymentInfo() {
        return execute("get packagedeployment/vcfa-bundle -o json");
    }

    public void apply(String updatedSpec) {
        execute(envvars, "apply -f -", updatedSpec, LogLevel.INFO);
    }

    public void tailLogFile(String podName, String logFile, int lineCount, boolean tailFollow) {
        String tailOption = tailFollow ? " -f" : "";
        execute(envvars, "exec {} -- tail -n {} {} {}", null, LogLevel.INFO,
                podName, String.valueOf(lineCount), tailOption, logFile);
    }

    public List<String> getPodStatus(String podName) {
        String output = execute(envvars, "get pods", LogLevel.DEBUG);
        return MatcherUtils.allMatches(output, "(" + podName + ".+?)\n");
    }

    @Override
    protected String executablePath() {
        String namespaceSuffix = StringUtils.isNotBlank(namespace) ? " --namespace=" + namespace : "";
        return "kubectl" + namespaceSuffix;
    }
}

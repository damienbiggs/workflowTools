package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class KubectlConfig {
    @ConfigurableProperty(commandLine = "--kube-config", help = "Config file for kubectl")
    public String kubeConfigFile;

    @ConfigurableProperty(commandLine = "--namespace", help = "Namespace to use")
    public String namespace;

    @ConfigurableProperty(commandLine = "--pod-name", help = "Name of kubernetes pod to use")
    public String podName;

    @ConfigurableProperty(commandLine = "--registry-url", help = "Kubernetes registry to use")
    public String registryUrl;

    @ConfigurableProperty(commandLine = "--package-update-path", help = "Relative path within a build for the required deliverable")
    public String packageUpdatePath;
}

package com.vmware.config.section;

import java.util.Map;

import com.google.gson.annotations.Expose;
import com.vmware.config.CalculatedProperty;
import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;
import com.vmware.util.scm.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildwebConfig {

    @Expose(serialize = false, deserialize = false)
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @Expose(serialize = false, deserialize = false)
    private final Git git = new Git();

    @ConfigurableProperty(help = "Url for buildweb server")
    public String buildwebUrl;

    @ConfigurableProperty(help = "Path to gobuild bin file, this is a VMware specific tool")
    public String goBuildBinPath;

    @ConfigurableProperty(commandLine = "-buildwebProject,--buildweb-project", help = "Which buildweb project to use for a gobuild sandbox buikd, this is for a VMware specific tool")
    public String buildwebProject;

    @ConfigurableProperty(help = "Default buildweb branch to use")
    public String defaultBuildwebBranch;

    @ConfigurableProperty(commandLine = "--buildweb-branch",
            help = "Which branch on buildweb to use for a gobuild sandbox build, this is for a VMware specific tool",
            methodNameForValueCalculation = "determineBuildwebBranch")
    public String buildwebBranch;

    @ConfigurableProperty(commandLine = "--build-type", help = "Buildweb build type to use, this is for a VMware specific tool")
    public String buildType;

    @ConfigurableProperty(commandLine = "--sandbox-build-number", help = "Buildweb build number to use, this is for a VMware specific tool")
    public String sandboxBuildNumber;

    @ConfigurableProperty(commandLine = "--component-builds", help = "Component builds value, this is for a VMware specific tool")
    public String componentBuilds;

    @ConfigurableProperty(commandLine = "--store-trees", help = "Adds --store-trees to the gobuild command, this is for a VMware specific tool")
    public boolean storeTrees;

    @ConfigurableProperty(commandLine = "--buildweb-api-url", help = "Api Url for buildweb server")
    public String buildwebApiUrl;

    @ConfigurableProperty(commandLine = "--buildweb-log-file-name", help = "Name of log file for buildweb build")
    public String buildwebLogFileName;

    @ConfigurableProperty(commandLine = "--sync-to-branch-latest", help = "By default, files to be synced to the latest in perforce, this flag syncs them to the latest changelist known to the git branch")
    public boolean syncChangelistToLatestInBranch;

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--include-in-progress", help = "Display output for in progress builds")
    public boolean includeInProgressBuilds;

    @ConfigurableProperty(commandLine = "--build-display-name", help = "Display name to use for the buildweb build invoked")
    public String buildDisplayName;

    @ConfigurableProperty(commandLine = "--exclude-sync-to", help = "Exclude sync-to parameter in gobuild command")
    public boolean excludeSyncTo;

    @ConfigurableProperty(help = "Git tracking branch mappings to buildweb branches")
    public Map<String, String> gitTrackingBranchMappings;

    @ConfigurableProperty(commandLine = "--use-git-tracking-branch", help = "Use git tracking branch as tracking branch for review")
    public boolean useGitTrackingBranch;

    public CalculatedProperty determineBuildwebBranch() {
        if (StringUtils.isNotBlank(buildwebBranch)) {
            return new CalculatedProperty(buildwebBranch, "buildwebBranch");
        }
        if (!git.workingDirectoryIsInGitRepo()) {
            return new CalculatedProperty(defaultBuildwebBranch, "defaultBuildwebBranch");
        }

        String trackingBranch = git.getTrackingBranch();
        if (StringUtils.isEmpty(trackingBranch)) {
            return new CalculatedProperty(defaultBuildwebBranch, "defaultBuildwebBranch");
        }
        String trackingBranchWithoutOrigin = trackingBranch.substring(trackingBranch.indexOf("/") + 1);
        log.debug("Parsed branch name {} from tracking branch {}", trackingBranchWithoutOrigin, trackingBranch);
        if (gitTrackingBranchMappings != null && gitTrackingBranchMappings.containsKey(trackingBranchWithoutOrigin)) {
            log.debug("Using mapping value {} for branch name {}",
                    gitTrackingBranchMappings.get(trackingBranchWithoutOrigin), trackingBranchWithoutOrigin);
            return new CalculatedProperty(gitTrackingBranchMappings.get(trackingBranchWithoutOrigin), "git tracking branch mapping");
        }

        if (useGitTrackingBranch) {
            log.debug("Using tracking branch name {} as buildweb branch since useGitTrackingBranch is set to true", trackingBranchWithoutOrigin);
            return new CalculatedProperty(trackingBranchWithoutOrigin, "git tracking branch");
        }

        return new CalculatedProperty(defaultBuildwebBranch, "defaultBuildwebBranch");
    }
}

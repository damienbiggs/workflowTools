package com.vmware.action.buildweb;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.buildweb.domain.BuildwebBuild;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.SystemUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the log page for a Buildweb sandbox build")
public class OpenSandboxLogPage extends BaseCommitWithBuildwebBuildsAction {

    public OpenSandboxLogPage(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl);

        if (matchingBuilds.size() == 1) {
            log.info("Opening build {} as it is the only Buildweb build", matchingBuilds.get(0).name);
            BuildwebBuild build = buildweb.getSandboxBuild(matchingBuilds.get(0).buildNumber());
            SystemUtils.openUrl(buildweb.getLogsUrl(build));
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.name));
            int selection = InputUtils.readSelection(choices, "Select sandbox build to open");
            BuildwebBuild build = buildweb.getSandboxBuild(matchingBuilds.get(selection).buildNumber());
            SystemUtils.openUrl(buildweb.getLogsUrl(build));
        }



    }
}

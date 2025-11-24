package com.vmware.action.commitInfo;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

@ActionDescription("Change the build number for the selected build")
public class ChangeBuildNumber extends BaseCommitAction {

    public ChangeBuildNumber(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = getAllJobBuilds();
        log.info("");
        matchingBuilds.add(new JobBuild() {
            @Override
            public String getLabel() {
                return "none";
            }
        });

        int selection = InputUtils.readSelection(matchingBuilds.toArray(new InputListSelection[0]),
                "Select build to change build number for");
        if (selection >= matchingBuilds.size()  - 1) {
            return;
        }

        JobBuild selectedBuild = matchingBuilds.get(selection);
        log.info("Existing url {} and build number {}", selectedBuild.url, selectedBuild.number());
        selectedBuild.updateBuildNumber(InputUtils.readValueUntilValidInt("Enter new build number"));
    }
}

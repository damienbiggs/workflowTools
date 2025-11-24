package com.vmware.action.commitInfo;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

@ActionDescription("Removes selected builds from testing done section of commit.")
public class RemoveSelectedBuilds extends BaseCommitAction {

    public RemoveSelectedBuilds(WorkflowConfig config) {
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
        List<Integer> selections = InputUtils.readSelections(matchingBuilds.toArray(new InputListSelection[0]),
                "Select builds to remove from commit", false);
        // check selection doesn't contain none value
        if (!selections.contains(matchingBuilds.size() - 1)) {
            selections.forEach(selection -> draft.jobBuilds.removeIf(build -> build.url.equals(matchingBuilds.get(selection).url)));
        }
    }
}

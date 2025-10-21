package com.vmware.action.commitInfo;

import com.vmware.BuildStatus;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.logging.Padder;

import java.util.List;

@ActionDescription("Display results for buildweb and jenkins builds")
public class DisplayBuildResults extends BaseCommitAction {
    public DisplayBuildResults(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl, buildwebConfig.buildwebUrl).isEmpty()) {
            skipActionDueTo("no builds found");
        }
    }

    @Override
    public void process() {
        Padder buildsPadder = new Padder("Build results");
        buildsPadder.infoTitle();
        List<JobBuild> builds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl, buildwebConfig.buildwebUrl);
        builds.forEach(build -> log.info("{} {} {}", build.name, build.url, build.status != null ? build.status : BuildStatus.UNKNOWN));
        buildsPadder.infoTitle();
    }
}

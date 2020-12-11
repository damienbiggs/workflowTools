package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuildArtifact;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.FileUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Download a specified artifact from a jenkins build.")
public class DownloadBuildArtifact extends BaseCommitWithJenkinsBuildsAction {
    public DownloadBuildArtifact(WorkflowConfig config) {
        super(config, true);
        super.addFailWorkflowIfBlankProperties("jobArtifact");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (!jenkinsConfig.hasConfiguredArtifact() && draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty()) {
            exitDueToFailureCheck("Jenkins artifact is not configured and there are no builds in the commit testing done section");
        }
    }

    @Override
    public void process() {
        JobBuild buildDetails = getJobBuildDetails();
        replacementVariables.addVariable(ReplacementVariables.VariableName.BUILD_NUMBER, buildDetails.number());

        String fullUrl = buildDetails.fullUrlForArtifact(jenkinsConfig.jobArtifact);
        JobBuildArtifact matchingArtifact = buildDetails.getArtifactForPathPattern(jenkinsConfig.jobArtifact);
        String downloadedFileName = FileUtils.appendToFileName(matchingArtifact.fileName, buildDetails.number());
        log.info("Downloading build artifact {}", fullUrl);
        fileSystemConfig.fileData = IOUtils.read(fullUrl);
        replacementVariables.addVariable(ReplacementVariables.VariableName.LAST_DOWNLOADED_FILE_NAME, downloadedFileName);
    }

    private JobBuild getJobBuildDetails() {
        JobBuild buildDetails;
        if (jenkinsConfig.hasConfiguredArtifact()) {
            log.info("Downloading artifact {} from job {} with build number {}", jenkinsConfig.jobArtifact,
                    jobWithArtifactName(), jenkinsConfig.jobBuildNumber);
            buildDetails = jenkins.getJobBuildDetails(jenkinsConfig.jobWithArtifact, jenkinsConfig.jobBuildNumber);
        } else {
            JobBuild build = determineBuildToUse();
            buildDetails = jenkins.getJobBuildDetails(build);
        }
        return buildDetails;
    }

    private JobBuild determineBuildToUse() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        if (draft.selectedBuild != null) {
            return matchingBuilds.get(draft.selectedBuild);
        }
        if (matchingBuilds.size() == 1) {
            log.info("Using build {} as it is the only Jenkins build", matchingBuilds.get(0).name);
            draft.selectedBuild = 0;
            return matchingBuilds.get(0);
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.name));
            int selection = InputUtils.readSelection(choices, "Select jenkins builds to download artifact " + jenkinsConfig.jobArtifact + " for");
            draft.selectedBuild = selection;
            return matchingBuilds.get(selection);
        }
    }
}
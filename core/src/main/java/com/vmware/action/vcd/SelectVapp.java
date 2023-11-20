package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobBuildArtifact;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription(value = "Select a specific Vapp.", configFlagsToAlwaysExcludeFromCompleter = "--source-database-schema-name")
public class SelectVapp extends BaseVappAction {
    public SelectVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(sshConfig.usesSshSite(), "ssh site is configured");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (vcdConfig.useOwnedVappsOnly) {
            super.failIfTrue(vappData.getVapps().isEmpty(), "no Vapps available");
        } else {
            super.failIfTrue(vappData.getVapps().isEmpty() && StringUtils.isEmpty(vcdConfig.vappJsonFile) && !jenkinsConfig.hasConfiguredArtifact(),
                    "no Vapps available");
        }
    }

    @Override
    public void process() {
        if (!vcdConfig.useOwnedVappsOnly && StringUtils.isNotEmpty(vcdConfig.vappJsonFile)) {
            log.info("Using Vapp json file {}", vcdConfig.vappJsonFile);
            vappData.setSelectedVapp(new QueryResultVappType("url", vcdConfig.vappJsonFile));
        } else if (!vcdConfig.useOwnedVappsOnly && jenkinsConfig.hasConfiguredArtifact()) {
            JobBuild buildDetails = serviceLocator.getJenkins().getJobBuildDetails(jobWithArtifactName(), jenkinsConfig.jobBuildNumber);
            JobBuildArtifact matchingArtifact = buildDetails.getArtifactForPathPattern(jenkinsConfig.jobArtifact);
            String jobArtifactPath = buildDetails.fullUrlForArtifact(matchingArtifact);
            log.info("Using artifact {}", jobArtifactPath);
            vappData.setSelectedVapp(new QueryResultVappType("artifact", jobArtifactPath));
        } else if (StringUtils.isNotEmpty(vcdConfig.vappName)) {
            log.info("Using specified Vapp name {}", vcdConfig.vappName);
            vappData.setSelectedVappByName(vcdConfig.vappName);
        } else if (!vappData.noVappSelected()) {
            log.info("Using already selected Vapp {}", vappData.getSelectedVappName());
        } else {
            int selectedVapp = InputUtils.readSelection(vappData.vappLabels(),
                    "Select Vapp (Total VM count " + vappData.totalVMs() + ")");
            vappData.setSelectedVappByIndex(selectedVapp);
        }
        if (StringUtils.isNotBlank(vappData.getSelectedVappName())) {
            String vappNameWithoutPeriods = vappData.getSelectedVappName().replace(".", "");
            replacementVariables.addVariable(ReplacementVariables.VariableName.VAPP_NAME, vappNameWithoutPeriods);
        }
    }
}

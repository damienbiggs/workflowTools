package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.SystemUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the web page for a Jenkins Build")
public class OpenBuildPage extends BaseCommitWithJenkinsBuildsAction {

    public OpenBuildPage(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        String buildNameToOpen = jenkinsConfig.jobsDisplayNames != null && jenkinsConfig.jobsDisplayNames.length > 0 ?
                jenkinsConfig.jobsDisplayNames[0] : null;
        Optional<JobBuild> matchingBuild = buildNameToOpen != null ? matchingBuilds.stream()
                .filter(build -> buildNameToOpen.equalsIgnoreCase(build.getLabel())).findFirst() : Optional.empty();

        if (matchingBuild.isPresent()) {
            log.debug("Opening build {}", matchingBuild.get().getLabel());
            SystemUtils.openUrl(matchingBuild.get().url);
        } else if (buildNameToOpen != null) {
            throw new FatalException("No matching build found for {}", buildNameToOpen);
        } else if (matchingBuilds.size() == 1) {
            log.info("Opening build {} as it is the only Jenkins build", matchingBuilds.get(0).getLabel());
            String url = matchingBuilds.get(0).url;
            SystemUtils.openUrl(url);
        } else {
            List<InputListSelection> choices = matchingBuilds.stream().map(build -> ((InputListSelection) build)).collect(Collectors.toList());
            int selection = InputUtils.readSelection(choices, "Select jenkins builds to open web page for");

            String url = matchingBuilds.get(selection).url;
            SystemUtils.openUrl(url);
        }
    }
}

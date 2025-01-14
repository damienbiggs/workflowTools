package com.vmware.config.section;

import com.google.gson.annotations.JsonAdapter;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.config.jira.IssueTypesDefinitionMapper;
import com.vmware.util.UrlUtils;

import java.util.Map;

public class JiraConfig {

    @ConfigurableProperty(commandLine = "--jira-url", help = "Url for jira server")
    public String jiraUrl;

    @ConfigurableProperty(commandLine = "-disableJira,--disable-jira", help = "Don't use Jira when checking bug numbers")
    public boolean disableJira;

    @ConfigurableProperty(help = "Default Jira project to use")
    public String defaultJiraProject;

    @ConfigurableProperty(commandLine = "--epic-id", help = "Jira epic to use")
    public String epicId;

    @ConfigurableProperty(commandLine = "-defaultJiraComponent,--default-jira-component", help = "Default Jira component to use for creating issues")
    public String defaultJiraComponent;

    @ConfigurableProperty(commandLine = "--include-sprint-stories", help = "When loading jira issues, use this flag to also include stories in sprints")
    public boolean includeSprintStories;

    @ConfigurableProperty(commandLine = "--include-all-issue-types", help = "When loading jira issues, use this flag to include issues of all types")
    public boolean includeAllIssueTypes;

    @ConfigurableProperty(commandLine = "--include-estimated", help = "Whether to include stories already estimated when loading jira issues for processing")
    public boolean includeStoriesWithEstimates;

    @JsonAdapter(IssueTypesDefinitionMapper.class)
    @ConfigurableProperty(commandLine = "--include-issue-types", help = "Specific issue types to include")
    public IssueTypeDefinition[] issueTypesToInclude;

    @ConfigurableProperty(help = "Custom field names for Jira")
    public Map<String, String> jiraCustomFieldNames;

    @ConfigurableProperty(commandLine = "--estimate", help = "Number of hours to set as the estimate for jira tasks.")
    public int jiraTaskEstimateInHours;

    @ConfigurableProperty(commandLine = "--use-label", help = "Whether to use a jira label when loading Jira issues")
    public boolean useLabel;

    @ConfigurableProperty(commandLine = "--use-fix-version", help = "Whether to use a jira fix by version when loading Jira issues")
    public boolean useFixVersion;

    @ConfigurableProperty(commandLine = "--use-epics", help = "Whether to use parent epics when loading Jira issues")
    public boolean useEpics;

    @ConfigurableProperty(commandLine = "--jira-username", help = "Username for Jira if not using the default username")
    public String jiraUsername;

    public String issueUrl(String bugNumber) {
        return UrlUtils.addRelativePaths(jiraUrl, "browse", bugNumber);
    }
}

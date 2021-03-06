package com.vmware.config;

import com.vmware.scm.Git;
import com.vmware.ServiceLocator;
import com.vmware.action.BaseAction;
import com.vmware.util.ArrayUtils;
import com.vmware.util.CommitConfiguration;
import com.vmware.util.StringUtils;

import com.google.gson.annotations.Expose;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import static com.vmware.util.ArrayUtils.contains;

/**
 * Workflow configuration.
 * All configuration is contained in this class.
 */
public class WorkflowConfig {

    private static Logger log = LoggerFactory.getLogger(WorkflowConfig.class.getName());

    @Expose(serialize = false, deserialize = false)
    private Git git = new Git();

    @Expose(serialize = false, deserialize = false)
    public Map<String, String> overriddenConfigSources = new TreeMap<String, String>();

    @Expose(serialize = false, deserialize = false)
    public List<Class<? extends BaseAction>> workFlowActions;

    @Expose(serialize = false, deserialize = false)
    public List<Field> configurableFields = new ArrayList<Field>();

    @Expose(serialize = false, deserialize = false)
    public String loadedConfigFiles;

    @ConfigurableProperty(help = "Label for testing done section")
    public String testingDoneLabel;

    @ConfigurableProperty(help = "Label for bug number")
    public String bugNumberLabel;

    @ConfigurableProperty(help = "Label for reviewed by")
    public String reviewedByLabel;

    @ConfigurableProperty(help = "Label for review url")
    public String reviewUrlLabel;

    @ConfigurableProperty(help = "Label for no bug number")
    public String noBugNumberLabel;

    @ConfigurableProperty(help = "Label for trivial reviewer")
    public String trivialReviewerLabel;

    @ConfigurableProperty(commandLine = "-u,--username", help = "Username to use for jenkins, jira and review board")
    public String username;

    @ConfigurableProperty(commandLine = "-reviewboardUrl,--reviewboard-url", help = "Url for review board server", gitConfigProperty = "reviewboard.url")
    public String reviewboardUrl;

    @ConfigurableProperty(commandLine = "-reviewRepo,--review-board-repo", help = "Review board repository")
    public String reviewBoardRepository;

    @ConfigurableProperty(commandLine = "-rbDateFormat,--review-board-date-format", help = "Date format used by review board. Can change between review board versions")
    public String reviewBoardDateFormat;

    @ConfigurableProperty(commandLine = "-jenkinsUrl,--jenkins-url", help = "Url for jenkins server")
    public String jenkinsUrl;

    @ConfigurableProperty(commandLine = "-buildwebApiUrl,--buildweb-url", help = "Api Url for buildweb server")
    public String buildwebApiUrl;

    @ConfigurableProperty(commandLine = "-jcsrf,--jenkins-uses-csrf", help = "Whether the jenkins server uses CSRF header")
    public boolean jenkinsUsesCsrf;

    @ConfigurableProperty(commandLine = "-jiraUrl,--jira-url", help = "Url for jira server")
    public String jiraUrl;

    @ConfigurableProperty(commandLine = "-jiraTestIssue,--jira-test-issue", help = "Issue key to fetch to test user is logged in")
    public String jiraTestIssue;

    @ConfigurableProperty(commandLine = "-disableJira,--disable-jira", help = "Don't use Jira when checking bug numbers")
    public boolean disableJira;

    @ConfigurableProperty(commandLine = "-bugzillaUrl,--bugzilla-url", help = "Url for Bugzilla server")
    public String bugzillaUrl;

    @ConfigurableProperty(commandLine = "-bugzillaTestBug,--bugzilla-test-bug", help = "Bug number to fetch to test user is logged in")
    public int bugzillaTestBug;

    @ConfigurableProperty(commandLine = "-disableBugzilla,--disable-bugzilla", help = "Don't use Bugzilla when checking bug numbers")
    public boolean disableBugzilla;

    @ConfigurableProperty(commandLine = "-bugzillaQuery,--bugzilla-query", help = "Named query in bugzilla to execute for loading assigned bugs")
    public String bugzillaQuery;

    @ConfigurableProperty(commandLine = "-trelloUrl,--trello-url", help = "Url for trello server")
    public String trelloUrl;

    @ConfigurableProperty(commandLine = "-p4Client,--perforce-client", gitConfigProperty = "git-p4.client", help = "Perforce client to use")
    public String perforceClientName;

    @ConfigurableProperty(commandLine = "-syncToBranchLatest,--sync-to-branch-latest", help = "By default, files to be synced to the latest in perforce, this flag syncs them to the latest changelist known to the git branch")
    public boolean syncChangelistToLatestInBranch;

    @ConfigurableProperty(commandLine = "-gobuildBinPath,--gobuild-bin-path", help = "Path to gobuild bin file, this is a VMware specific tool")
    public String goBuildBinPath;

    @ConfigurableProperty(commandLine = "-buildwebProject,--buildweb-project", help = "Which buildweb project to use for a gobuild sandbox buikd, this is for a VMware specific tool")
    public String buildwebProject;

    @ConfigurableProperty(commandLine = "-buildwebBranch,--sandbox-branch", help = "Which branch on buildweb to use for a gobuild sandbox build, this is for a VMware specific tool")
    public String buildwebBranch;

    @ConfigurableProperty(commandLine = "--include-estimated", help = "Whether to include stories already estimated when loading jira issues for processing")
    public boolean includeStoriesWithEstimates;

    @ConfigurableProperty(help = "Swimlanes to use for trello. A list of story point values as integers is expected")
    public SortedSet<Integer> storyPointValues;

    @ConfigurableProperty(help = "Map of user friendly names for jenkins jobs to select from")
    public Map<String, String> jenkinsJobsMappings = new HashMap<>();

    @ConfigurableProperty(commandLine = "-waitForJenkins,--wait-for-jenkins", help = "Waits for jenkins job to complete, when running multiple jobs, waits for previous one to complete before starting next one")
    public boolean waitForJenkinsJobCompletion;

    @ConfigurableProperty(commandLine = "-ignoreJobFailure,--ignore-jenkins-job-failure", help = "If wait for Jenkins job result is set, then ignore job failure and run the next build")
    public boolean ignoreJenkinsJobFailure;

    @ConfigurableProperty(help = "Max number of jenkins jobs to iterate over when checking for latest status of jenkins job")
    public int maxJenkinsBuildsToCheck;

    @ConfigurableProperty(help = "Array of reviewers to select from for reviewed by section")
    public SortedSet<String> targetReviewers;

    @ConfigurableProperty(help = "Map of reviewer groups to select from for reviewed by section. E.g. create a techDebt group and list relevant reviewers")
    public LinkedHashMap<String, SortedSet<String>> reviewerGroups;

    @ConfigurableProperty(help = "Default value to set for topic if none entered")
    public String defaultTopic;

    @ConfigurableProperty(help = "Template values for topic, press up to cycle through values when entering topic")
    public String[] topicTemplates;

    @ConfigurableProperty(help = "Template values for testing done, press up to cycle through values when entering testing done")
    public String[] testingDoneTemplates;

    @ConfigurableProperty(commandLine = "-g,--groups", help = "Groups to set for the review request or for generating stats")
    public String[] targetGroups;

    @ConfigurableProperty(commandLine = "-tb,--tracking-branch", help = "Tracking branch to use as base for reviews")
    public String trackingBranch;

    @ConfigurableProperty(commandLine = "-p,--parent", help = "Parent branch to use for the git diff to upload to review board")
    public String parentBranch;

    @ConfigurableProperty(commandLine = "-b,--branch", help = "Optional value to set if using the local branch name for review board is not desired")
    public String targetBranch;

    @ConfigurableProperty(commandLine = "-defaultJiraProject,--default-jira-project", help = "Default Jira project to use")
    public String defaultJiraProject;

    @ConfigurableProperty(commandLine = "-defaultJiraComponent,--default-jira-component", help = "Default Jira component to use for creating issues")
    public String defaultJiraComponent;

    @ConfigurableProperty(help = "Order of services to check against for bug number")
    public String[] bugNumberSearchOrder;

    @ConfigurableProperty(commandLine = "-bugzillaPrefix,--bugzilla-prefix", help = "Represents a bug in bugzilla, only the number part will be stored")
    public String bugzillaPrefix;

    @ConfigurableProperty(commandLine = "-t,--trace", help = "Sets log level to trace")
    public boolean traceLogLevel;

    @ConfigurableProperty(commandLine = "-d,--debug", help = "Sets log level to debug")
    public boolean debugLogLevel;

    @ConfigurableProperty(commandLine = "-l, ,--log, --log-level", help = "Sets log level to any of the following, SEVERE,INFO,FINE,FINER,FINEST")
    public String logLevel;

    @ConfigurableProperty(commandLine = "-ms,--max-summary", help = "Sets max line length for the one line summary")
    public int maxSummaryLength;

    @ConfigurableProperty(commandLine = "-md,--max-description", help = "Sets max line length for all other lines in the commit")
    public int maxDescriptionLength;

    @ConfigurableProperty(commandLine = "-j,--jenkins-jobs", help = "Sets the names and parameters for the jenkins jobs to invoke. Separate jobs by commas and parameters by ampersands")
    public String jenkinsJobsToUse;

    @ConfigurableProperty(commandLine = "--estimate", help = "Number of hours to set as the estimate for jira tasks.")
    public int jiraTaskEstimateInHours;

    @ConfigurableProperty(commandLine = "--old-submit", help = "Number of days after which to close old reviews that have been soft submitted")
    public int closeOldSubmittedReviewsAfter;

    @ConfigurableProperty(commandLine = "--old-ship-it", help = "Number of days after which to close old reviews that have ship its")
    public int closeOldShipItReviewsAfter;

    @ConfigurableProperty(commandLine = "--disable-jenkins-login", help = "Skips trying to log into jenkins if the server is not using user login module")
    public boolean disableJenkinsLogin;

    @ConfigurableProperty(help = "Map of remote branches, :username is substituted for the real username.")
    public TreeMap<String, String> remoteBranches;

    @ConfigurableProperty(commandLine = "-rb, --remote-branch", help = "Remote branch name to use")
    public String remoteBranchToUse;

    @ConfigurableProperty(help = "A map of workflows that can be configured. A workflow comprises a list of workflow actions.")
    public TreeMap<String, String[]> workflows;

    @ConfigurableProperty(help = "A list of workflows that are only created for supporting other workflows. Adding them here hides them on initial auto complete")
    public List<String> supportingWorkflows;

    @ConfigurableProperty(commandLine = "-w,--workflow", help = "Workflows / Actions to run")
    public String workflowsToRun;

    @ConfigurableProperty(commandLine = "-dr,--dry-run", help = "Shows the workflow actions that would be run")
    public boolean dryRun;

    @ConfigurableProperty(commandLine = "--set-empty-only", help = "Set values for empty properties only. Ignore properties that already have values")
    public boolean setEmptyPropertiesOnly;

    @ConfigurableProperty(commandLine = "-pid,--patch", help = "Id of the review request to use for patching")
    public int reviewRequestForPatching;

    @ConfigurableProperty(commandLine = "--latest-diff", help = "Always use latest diff from review request for patching")
    public boolean alwaysUseLatestDiff;

    @ConfigurableProperty(commandLine = "--use-label", help = "Whether to use a jira label when creating a trello board")
    public boolean useJiraLabel;

    @ConfigurableProperty(commandLine = "-kmc,--keep-missing-cards", help = "Whether to not delete existing cards in Trello that do not match a loaded Jira issue")
    public boolean keepMissingCards;

    @ConfigurableProperty(commandLine = "-obo,--own-boards-only", help = "Disallow using a trello board owned by someone else")
    public boolean ownBoardsOnly;

    @ConfigurableProperty(commandLine = "--file-count-ranges", help = "File count ranges for grouping reviews when generating stats")
    public int[] fileCountRanges;

    @ConfigurableProperty(commandLine = "--wait-time", help = "Wait time for blocking workflow action to complete.")
    public long waitTimeForBlockingWorkflowAction;

    @ConfigurableProperty(help = "Variables to use for jenkins jobs, can set specific values re command line as well, e.g. -JVAPP_NAME=test -JUSERNAME=dbiggs")
    public Map<String, String> jenkinsJobParameters = new TreeMap<>();

    @Expose(serialize = false, deserialize = false)
    private ServiceLocator serviceLocator = null;

    public WorkflowConfig() {}

    public ServiceLocator configuredServiceLocator() {
        if (serviceLocator == null) {
            serviceLocator = new ServiceLocator(this);
        }
        return serviceLocator;
    }

    public void generateConfigurablePropertyList() {
        List<String> usedParams = new ArrayList<String>();
        for (Field field : WorkflowConfig.class.getFields()) {
            ConfigurableProperty configProperty = field.getAnnotation(ConfigurableProperty.class);
            if (configProperty == null) {
                continue;
            }
            String[] params = configProperty.commandLine().split(",");
            for (String param : params) {
                if (param.equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                    continue;
                }
                if (usedParams.contains(param)) {
                    throw new RuntimeException(
                            String.format("Config flag %s has already been set to configure another property", param));
                }
                usedParams.add(param);
            }
            configurableFields.add(field);
        }
    }

    public void applyRuntimeArguments(CommandLineArgumentsParser argsParser) {
        workFlowActions = new WorkflowActionLister().findWorkflowActions();
        List<ConfigurableProperty> commandLineProperties = applyConfigValues(argsParser.getArgumentMap(), "Command Line");
        if (argsParser.containsArgument("--possible-workflow")) {
            overriddenConfigSources.put("workflowsToRun", "Command Line");
            this.workflowsToRun = argsParser.getExpectedArgument("--possible-workflow");
        }
        argsParser.checkForUnrecognizedArguments(commandLineProperties);
    }

    public LogLevel determineLogLevel() {
        if (traceLogLevel) {
            logLevel = LogLevel.TRACE.name();
        } else if (debugLogLevel) {
            logLevel = LogLevel.DEBUG.name();
        }
        return LogLevel.valueOf(logLevel);
    }

    public int getSearchOrderForService(String serviceToCheckFor) {
        for (int i = 0; i < bugNumberSearchOrder.length; i ++) {
            if (bugNumberSearchOrder[i].equalsIgnoreCase(serviceToCheckFor)) {
               return i;
            }
        }
        return -1;
    }


    public List<Class<? extends BaseAction>> determineActions(String workflowString) {
        String[] possibleActions = workflows.get(workflowString);
        if (possibleActions != null) {
            log.info("Using workflow {}", workflowString);
            log.debug("Using workflow values {}", Arrays.toString(possibleActions));
        } else {
            log.info("Treating workflow argument {} as a custom workflow string as it did not match any existing workflows", workflowString);
            possibleActions = workflowString.split(",");
        }
        log.info("");

        WorkflowValuesParser valuesParser = new WorkflowValuesParser(workflows, workFlowActions);
        valuesParser.parse(possibleActions);
        applyConfigValues(valuesParser.getConfigValues(), "Config in Workflow");
        if (!valuesParser.getUnknownActions().isEmpty()) {
            throw new UnknownWorkflowValueException(valuesParser.getUnknownActions());
        }
        return valuesParser.getActionClasses();
    }

    /**
     * Set separate to other git config values as it shouldn't override a specific workflow file configuration.
     */
    public void setGitOriginUrlAsReviewBoardRepo() {
        String gitOriginValue = git.configValue("remote.origin.url");
        if (StringUtils.isBlank(gitOriginValue)) {
            return;
        }

        log.debug("Setting git origin value {} as the reviewboard repository", gitOriginValue);
        reviewBoardRepository = gitOriginValue;
    }

    public void applyGitConfigValues() {
        Map<String, String> configValues = git.configValues();
        for (Field field : configurableFields) {
            ConfigurableProperty configurableProperty = field.getAnnotation(ConfigurableProperty.class);
            String configValueName = configurableProperty.gitConfigProperty().isEmpty() ? "workflow." + field.getName().toLowerCase()
                    : configurableProperty.gitConfigProperty();
            String value = configValues.get(configValueName);
            setFieldValue(field, value, "Git Config");
        }
    }

    public void overrideValues(WorkflowConfig overriddenConfig, String configFileName) {
        for (Field field : configurableFields) {
            Object existingValue;
            Object value;
            try {
                existingValue = field.get(this);
                value = field.get(overriddenConfig);
            } catch (IllegalAccessException e) {
                throw new RuntimeReflectiveOperationException(e);
            }
            if (value == null || String.valueOf(value).equals("0") || (value instanceof Boolean && !((Boolean) value))) {
                continue;
            }
            // copy values to default config map if value is a map
            if (existingValue != null && value instanceof Map) {
                Map valueMap = (Map) value;
                if (valueMap.isEmpty()) {
                    continue;
                }
                Map existingValues = (Map) existingValue;
                String existingConfigValue = overriddenConfigSources.get(field.getName());
                String updatedConfigValue;
                if (existingConfigValue == null && !existingValues.isEmpty()) {
                    updatedConfigValue = "default, " + configFileName;
                } else if (existingConfigValue == null) {
                    updatedConfigValue = configFileName;
                } else {
                    updatedConfigValue = existingConfigValue + ", " + configFileName;
                }
                overriddenConfigSources.put(field.getName(), updatedConfigValue);
                existingValues.putAll(valueMap);
            } else {
                overriddenConfigSources.put(field.getName(), configFileName);
                // override for everything else
                try {
                    field.set(this, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeReflectiveOperationException(e);
                }
            }
        }
    }

    public void parseUsernameFromGitEmailIfBlank() {
        if (StringUtils.isNotBlank(username)) {
            return;
        }
        String gitUserEmail = git.configValue("user.email");
        if (StringUtils.isNotBlank(gitUserEmail) && gitUserEmail.contains("@")) {
            this.username = gitUserEmail.substring(0, gitUserEmail.indexOf("@"));
            log.info("No username set, parsed username {} from git config user.email {}", username, gitUserEmail);
            overriddenConfigSources.put("username", "Git Config");
        }
    }

    public Field getMatchingField(String commandLineProperty) {
        for (Field field : configurableFields) {
            ConfigurableProperty property = field.getAnnotation(ConfigurableProperty.class);
            if (property.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }

            if (ArrayUtils.contains(property.commandLine().split(","), commandLineProperty)) {
                return field;
            }
        }
        return null;
    }

    public CommitConfiguration getCommitConfiguration() {
        return new CommitConfiguration(reviewboardUrl, jenkinsUrl, buildwebApiUrl, testingDoneLabel, bugNumberLabel,
                reviewedByLabel, reviewUrlLabel);
    }

    public JenkinsJobsConfig getJenkinsJobsConfig() {
        jenkinsJobParameters.put(JenkinsJobsConfig.USERNAME_PARAM, username);
        Map<String, String> presetParams = Collections.unmodifiableMap(jenkinsJobParameters);
        Map<String, String> jobMappings = Collections.unmodifiableMap(jenkinsJobsMappings);
        return new JenkinsJobsConfig(jenkinsJobsToUse, presetParams, jenkinsUrl, jobMappings);
    }

    public Integer parseBugzillaBugNumber(String bugNumber) {
        if (StringUtils.isInteger(bugNumber)) {
            return Integer.parseInt(bugNumber);
        }

        boolean prefixMatches = StringUtils.isNotBlank(bugzillaPrefix)
                && bugNumber.toUpperCase().startsWith(bugzillaPrefix.toUpperCase());
        if (!prefixMatches) {
            return null;
        }

        int lengthToStrip = bugzillaPrefix.length();
        if (bugNumber.toUpperCase().startsWith(bugzillaPrefix.toUpperCase() + "-")) {
            lengthToStrip++;
        }

        String numberPart = bugNumber.substring(lengthToStrip);
        if (StringUtils.isInteger(numberPart)) {
            return Integer.parseInt(numberPart);
        } else {
            return null;
        }
    }

    private void setFieldValue(Field field, String value, String source) {
        Object validValue = new WorkflowField(field).determineValue(value);
        if (validValue != null) {
            overriddenConfigSources.put(field.getName(), source);
            try {
                field.set(this, validValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeReflectiveOperationException(e);
            }
        }
    }

    private List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source) {
        if (configValues.isEmpty()) {
            return Collections.emptyList();
        }
        for (String configValue : configValues.keySet()) {
            if (!configValue.startsWith("-J")) {
                continue;
            }
            String parameterName = configValue.substring(2);
            String parameterValue = configValues.get(configValue);
            jenkinsJobParameters.put(parameterName, parameterValue);
        }
        List<ConfigurableProperty> propertiesAffected = new ArrayList<>();
        for (Field field : configurableFields) {
            ConfigurableProperty configurableProperty = field.getAnnotation(ConfigurableProperty.class);
            if (configurableProperty.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }
            for (String configValue : configValues.keySet()) {
                String[] commandLineArguments = configurableProperty.commandLine().split(",");
                if (contains(commandLineArguments, configValue)) {
                    propertiesAffected.add(configurableProperty);
                    String value = configValues.get(configValue);
                    if (value == null && (field.getType() == Boolean.class || field.getType() == boolean.class)) {
                        value = Boolean.TRUE.toString();
                    }
                    setFieldValue(field, value, source);
                }
            }
        }
        return propertiesAffected;
    }
}

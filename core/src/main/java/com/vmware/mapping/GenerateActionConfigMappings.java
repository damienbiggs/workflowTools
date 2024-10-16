package com.vmware.mapping;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.WorkflowActionLister;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowField;
import com.vmware.config.WorkflowFields;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.CommitConfig;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.ReviewBoardConfig;
import com.vmware.config.section.SectionConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.FileUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

import com.vmware.util.logging.WorkflowConsoleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;

/**
 * This class in run by the build to generate the action config mappings.
 * Specifically it creates a json file where each action has a list of config values that are relevant for that action.
 * This is done by a regex pattern that matches "config.[configValueName]
 * This works because the workflow config class is expected to be called config in all the action classes.
 */
public class GenerateActionConfigMappings {

    private static Map<String, List<String>> methodNameToConfigValueMappings = new HashMap<>();

    static {
        methodNameToConfigValueMappings.put("parentBranchPath", singletonList("parentBranch"));
        methodNameToConfigValueMappings.put("trackingBranchPath", singletonList("trackingBranch"));
        methodNameToConfigValueMappings.put("commandLineSite", Arrays.asList("sshHost", "sshPort", "sshUsername", "sshPassword"));
        methodNameToConfigValueMappings.put("determineBuildwebBranch", Arrays.asList("buildwebBranch", "useGitTrackingBranch"));
        methodNameToConfigValueMappings.put("queryFilters", singletonList("queryFilter"));
    }

    private static final Logger log = LoggerFactory.getLogger(GenerateActionConfigMappings.class);

    private static final Pattern serviceLocatorMethodPattern = Pattern.compile("[^\\.]serviceLocator\\.(\\w+)");

    private Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();

    private final Map<String, List<String>> locatorMethodArguments = new HashMap<>();

    private Pattern configValuePattern;

    private final File actionDirectory;

    private final File serviceLocatorFile;

    private final File outputFile;

    public GenerateActionConfigMappings(String actionDirectory, String outputFile, File serviceLocatorFile) {
        this.serviceLocatorFile = serviceLocatorFile;
        this.actionDirectory = new File(actionDirectory);
        this.outputFile = new File(outputFile);
    }

    public void run() {
        WorkflowConfig config = new WorkflowConfig();

        List<File> javaActionFiles = FileUtils.scanDirectorRecursivelyForFiles(actionDirectory, new JavaFileFilter());
        if (javaActionFiles.isEmpty()) {
            log.info("No action files found at {}", actionDirectory.getPath());
            return;
        }

        createConfigValuesPattern();

        Map<String, String[]> mappings = new HashMap<>();
        populateLocatorMethodArguments();
        WorkflowFields configurableFields = config.getConfigurableFields();
        for (File javaActionFile : javaActionFiles) {
            String className = FileUtils.stripExtension(javaActionFile);
            List<String> foundConfigValues = parseFileForConfigValues(javaActionFile);
            List<String> commandLineOptions = convertToCommandLineValues(foundConfigValues, configurableFields.values());
            if (!commandLineOptions.isEmpty()) {
                mappings.put(className, commandLineOptions.toArray(new String[commandLineOptions.size()]));
            }
        }
        String jsonOutput = gson.toJson(mappings);
        IOUtils.write(outputFile, jsonOutput);
    }

    private List<String> convertToCommandLineValues(List<String> foundConfigValues, List<WorkflowField> configurableFields) {
        List<String> commandLineOptions = new ArrayList<String>();

        for (int i = foundConfigValues.size() - 1; i >= 0; i--) {
            String foundConfigValue = foundConfigValues.get(i);
            if (methodNameToConfigValueMappings.containsKey(foundConfigValue)) {
                foundConfigValues.addAll(methodNameToConfigValueMappings.get(foundConfigValue));
            }
        }

        for (int i = foundConfigValues.size() -1; i >= 0; i --) {
            String foundConfigValue = foundConfigValues.get(i);
            boolean foundMatchingField = false;
            for (WorkflowField configurableField : configurableFields) {
                if (configurableField.getName().equals(foundConfigValue)) {
                    foundMatchingField = true;
                    ConfigurableProperty configurableProperty = configurableField.configAnnotation();
                    String commandLineText = configurableProperty.commandLine();
                    if (!commandLineText.equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                        String[] commandLineArgs = commandLineText.split(",");
                        commandLineOptions.add(commandLineArgs[commandLineArgs.length - 1]);
                    }
                    break;
                }
            }
            if (!foundMatchingField) {
                log.debug("No matching for config value {}, presuming it is a method", foundConfigValue);
            }
        }
        return commandLineOptions;
    }

    private List<String> parseFileForConfigValues(File javaActionFile) {
        String fileText = IOUtils.read(javaActionFile);
        Matcher configMatcher = configValuePattern.matcher(fileText);
        List<String> matches = new ArrayList<>();
        while (configMatcher.find()) {
            String foundValue = configMatcher.group(1);
            if (!matches.contains(foundValue)) {
                matches.add(foundValue);
            }
        }

        Matcher serviceLocatorMethodMatcher = serviceLocatorMethodPattern.matcher(fileText);
        while (serviceLocatorMethodMatcher.find()) {
            String foundValue = serviceLocatorMethodMatcher.group(1);
            List<String> configValues = locatorMethodArguments.get(foundValue);
            if (configValues == null) {
                continue;
            }

            for (String configValue : configValues) {
                if (!matches.contains(configValue)) {
                    matches.add(configValue);
                }
            }
        }

        return matches;
    }


    private class JavaFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathName) {
            return pathName.isDirectory() || pathName.getName().endsWith(".java");
        }
    }

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(new WorkflowConsoleHandler());
        globalLogger.setLevel(Level.INFO);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (args.length == 0) {
            log.error("Module base directory not specified");
            System.exit(1);
        }

        validateAllActionsCanBeInstantiated();

        String moduleBaseDirectory = args[0];
        String actionsDirectory =  platformIndependentPath(moduleBaseDirectory, "/src/main/java/com/vmware/action");
        String locatorFilePath = platformIndependentPath(moduleBaseDirectory, "/src/main/java/com/vmware/ServiceLocator.java");
        String mappingsFile = platformIndependentPath(moduleBaseDirectory, "/src/main/resources/configValueMappings.json");
        String targetDirectory = platformIndependentPath(moduleBaseDirectory, "/target/classes");
        log.info("Creating mappings file {} from folder {}", mappingsFile, actionsDirectory);

        GenerateActionConfigMappings generateActionConfigMappings =
                new GenerateActionConfigMappings(actionsDirectory, mappingsFile, new File(locatorFilePath));
        generateActionConfigMappings.run();
        log.info("Copying to target directory {}", targetDirectory);
        FileUtils.copyFile(new File(mappingsFile),
                new File(targetDirectory + File.separator + "configValueMappings.json"));
    }

    private static String platformIndependentPath(String moduleBaseDirectory, String path) {
        return (moduleBaseDirectory + path).replaceAll("/", File.separator);
    }

    private void populateLocatorMethodArguments() {
        List<String> lines = IOUtils.readLines(serviceLocatorFile);
        String currentMethodName = null;
        List<String> currentArguments = null;
        for (String line : lines) {
            String matchedMethodName = MatcherUtils.singleMatch(line, "public \\w+ (\\w+)\\(");
            if (StringUtils.isNotBlank(matchedMethodName)) {
                if (StringUtils.isNotBlank(currentMethodName)) {
                    locatorMethodArguments.put(currentMethodName, currentArguments);
                }
                currentMethodName = matchedMethodName;
                currentArguments = new ArrayList<>();
            } else if (StringUtils.isNotBlank(currentMethodName)) {
                Matcher configValueMatcher = configValuePattern.matcher(line);
                while (configValueMatcher.find()) {
                    currentArguments.add(configValueMatcher.group(1));
                }
            }
        }
        if (currentMethodName != null) {
            locatorMethodArguments.put(currentMethodName, currentArguments);
        }
    }

    private void createConfigValuesPattern() {
        List<String> sectionConfigFields = Arrays.stream(WorkflowConfig.class.getFields())
                .filter(field -> field.getAnnotation(SectionConfig.class) != null)
                .map(Field::getName).collect(Collectors.toList());
        List<String> sectionMatcherValues = sectionConfigFields.stream().map(value -> "config." + value).collect(Collectors.toList());
        sectionMatcherValues.addAll(sectionConfigFields);

        String configValuesPattern = String.join("|", sectionMatcherValues) + "|config";
        configValuePattern = Pattern.compile("[^.](?:" + configValuesPattern + ")\\.(\\w+)");
    }

    private static void validateAllActionsCanBeInstantiated() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<Class<? extends BaseAction>> workflowActions = new WorkflowActionLister().findWorkflowActions(classLoader);

        log.info("Validating all workflow actions can be instantiated");
        WorkflowConfig config = new WorkflowConfig();
        config.reviewBoardConfig = new ReviewBoardConfig();
        config.commitConfig = new CommitConfig();
        config.jenkinsConfig = new JenkinsConfig();
        config.buildwebConfig = new BuildwebConfig();

        workflowActions.forEach(action -> {
            try {
                BaseAction workflowAction = action.getConstructor(WorkflowConfig.class).newInstance(config);
                workflowAction.checkSkipAndFailConfigPropertiesAreValid();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

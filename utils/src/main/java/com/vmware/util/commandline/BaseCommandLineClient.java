package com.vmware.util.commandline;

import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.vmware.util.commandline.CommandLineUtils.executeCommand;
import static com.vmware.util.StringUtils.addArgumentsToValue;

/**
 * Common functionality for git, perforce and kubectl wrappers can be put in this superclass.
 */
public abstract class BaseCommandLineClient {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    File workingDirectory;

    CommandLineClientType clientType;

    BaseCommandLineClient(CommandLineClientType clientType) {
        this.clientType = clientType;
    }

    void setWorkingDirectory(File workingDirectory) {
        if (workingDirectory == null) {
            throw new FatalException("Cannot set null working directory for client "
                    + this.getClass().getSimpleName());
        }
        this.workingDirectory = workingDirectory;
    }

    void setWorkingDirectory(String workingDirectoryPath) {
        if (workingDirectoryPath == null) {
            throw new FatalException("Cannot set null working directory path for client "
                    + this.getClass().getSimpleName());
        }
        File directoryWithoutNormalizing = new File(workingDirectoryPath);
        try {
            this.setWorkingDirectory(directoryWithoutNormalizing.getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public String fullPath(String pathWithinScm) {
        return workingDirectory + File.separator + pathWithinScm;
    }

    String execute(String command, String... arguments) {
        return execute(command, LogLevel.DEBUG, arguments);
    }

    String executeShort(String command, String... arguments) {
        return execute(null, command, null, LogLevel.DEBUG, true, arguments);
    }

    String execute(String command, LogLevel logLevel, String... commandArguments) {
        return execute(command, null, logLevel, commandArguments);
    }

    String execute(Map<String, String> envvars, String command, LogLevel logLevel, String... commandArguments) {
        return execute(envvars, command, null, logLevel, commandArguments);
    }

    String execute(String command, String inputText, LogLevel level, String... commandArguments) {
        return execute(null, command, inputText, level, false, commandArguments);
    }

    String execute(Map<String, String> envvars, String command, String inputText, LogLevel level, String... commandArguments) {
        return execute(envvars, command, inputText, level, false, commandArguments);
    }

    String execute(Map<String, String> environmentVariables, String command, String inputText, LogLevel level, boolean logResultOnly, String... commandArguments) {
        String expandedCommand = executablePath() + " " + addArgumentsToValue(command, (Object[]) commandArguments);
        String output = executeCommand(workingDirectory, environmentVariables, expandedCommand, inputText, logResultOnly, level);
        String commandCheckOutput = checkIfCommandFailed(output);
        if (commandCheckOutput != null) {
            log.error(commandCheckOutput);
            System.exit(1);
        }
        return output;
    }

    String checkIfCommandFailed(String output) {
        return null;
    }

    String failOutputIfMissingText(String output, String expectedText) {
        if (!output.contains(expectedText)) {
            throw new RuntimeException("Expected to find text " + expectedText + " in output " + output);
        }
        return output;
    }

    String failOutputIfMissingText(String output, Collection<String> expectedTextOptions, int expectedCount) {
        int matches = 0;
        int currentIndex = 0;
        while (matches++ < expectedCount) {
            int matchIndex = -1;
            String matchedText = "";
            for (String expectedText : expectedTextOptions) {
                matchIndex = output.indexOf(expectedText, currentIndex);
                if (matchIndex != -1) {
                    matchedText = expectedText;
                    break;
                }
            }
            if (matchIndex == -1) {
                throw new RuntimeException("Unexpected output from command, none of"
                        + expectedTextOptions.toString() + " options were present\n" + output);
            }
            currentIndex = matchIndex + matchedText.length();
        }
        return output;
    }

    abstract String executablePath();

}

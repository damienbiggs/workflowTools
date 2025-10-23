package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.commandline.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.SkipActionException;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;
import com.vmware.util.commandline.scm.FileChange;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.util.StringUtils.appendWithDelimiter;
import static com.vmware.util.commandline.scm.FileChangeType.deleted;
import static com.vmware.util.commandline.scm.FileChangeType.deletedAfterRename;

public abstract class BaseCheckstyleAction extends BaseCommitAction {

    private static final String CHECKSTYLE_COMMAND_FORMAT = "java -Dcheckstyle.suppressions.file=%s -jar %s -c %s %s";

    boolean failIfCheckstyleFails;

    public BaseCheckstyleAction(WorkflowConfig config, boolean failIfCheckstyleFails) {
        super(config);
        super.addSkipActionIfBlankProperties("checkstyleJarPath", "checkstyleConfigXmlPath", "checkstyleSuppressionsXmlPath");
        this.failIfCheckstyleFails = failIfCheckstyleFails;
    }

    @Override
    public void process() {
        log.debug("Using checkstyle jar {} with config file {} and suppressions file {}",
                checkstyleConfig.checkstyleJarPath, checkstyleConfig.checkstyleConfigXmlPath,
                checkstyleConfig.checkstyleSuppressionsXmlPath);
        String checkstyleJarFullPath = createFullPath(checkstyleConfig.checkstyleJarPath);
        String checkstyleConfigXmlFullPath = createFullPath(checkstyleConfig.checkstyleConfigXmlPath);
        String checkstyleSuppressionsXmlFullPath = createFullPath(checkstyleConfig.checkstyleSuppressionsXmlPath);

        log.debug("Using checkstyle jar path {} with config file path {} and suppressions file path {}",
                checkstyleJarFullPath, checkstyleConfigXmlFullPath, checkstyleSuppressionsXmlFullPath);

        List<FileChange> fileChanges = getFileChangesInLastCommit();

        List<String> filePathsToCheck = fileChanges.stream().filter(fileChange -> fileChange.matchesNoneOf(deleted, deletedAfterRename))
                .filter(this::fileChangeStartsWithMapping)
                .map(fileChange -> createFullPath(fileChange.getLastFileAffected())).collect(Collectors.toList());
        String fileCheckText = "";
        for (String filePath : filePathsToCheck) {
            fileCheckText = appendWithDelimiter(fileCheckText, filePath, " ");
        }
        if (StringUtils.isEmpty(fileCheckText)) {
            log.info("No files need to be checked for checkstyle errors");
            return;
        }

        String checkstyleCommand = String.format(CHECKSTYLE_COMMAND_FORMAT,
                checkstyleSuppressionsXmlFullPath, checkstyleJarFullPath, checkstyleConfigXmlFullPath, fileCheckText);
        log.debug("Executing checkstyle command {}", checkstyleCommand);
        String checkstyleOutput = CommandLineUtils.executeCommand(checkstyleCommand, LogLevel.DEBUG);
        String errorCount = MatcherUtils.singleMatch(checkstyleOutput, "Checkstyle ends with (\\d+) errors.");
        if (errorCount != null) {
            String plural = Integer.parseInt(errorCount) == 1 ? "" : "s";
            String title = String.format("%s checkstyle error%s encountered", errorCount, plural);
            Padder errorPadder = new Padder(title);
            errorPadder.errorTitle();
            log.info(checkstyleOutput);
            errorPadder.errorTitle();
            if (failIfCheckstyleFails) {
                cancelWithMessage("checkstyle errors encountered");
            }
        } else {
            log.info("All files passed checkstyle check");
            log.debug(checkstyleOutput);
        }
    }

    private boolean fileChangeStartsWithMapping(FileChange fileChange) {
        if (!fileChange.getLastFileAffected().endsWith(".java")) {
            return false;
        }
        if (checkstyleConfig.checkstyleFileMappings == null || checkstyleConfig.checkstyleFileMappings.isEmpty()) {
            return true;
        }
        return checkstyleConfig.checkstyleFileMappings.stream()
                .anyMatch(fileMapping -> fileChange.getLastFileAffected().startsWith(fileMapping));
    }

    private String createFullPath(String relativePath) {
        File relativeFile = new File(relativePath);
        if (relativeFile.exists()) {
            return relativePath;
        }
        String fullPath;
        if (git.workingDirectoryIsInGitRepo()) {
            fullPath = git.fullPath(relativePath);
        } else if (reasonPerforceClientCannotBeUsed() == null) {
            fullPath = getLoggedInPerforceClient().fullPath(relativePath);
        } else {
            throw new SkipActionException("File path {} does not exist", relativePath);
        }
        if (!new File(fullPath).exists()) {
            throw new SkipActionException("File path {} does not exist", fullPath);
        }
        return fullPath;
    }

}

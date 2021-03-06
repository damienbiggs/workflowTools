package com.vmware.scm;

import java.util.Arrays;
import java.util.List;

public enum FileChangeType {

    added("A", "add", "added %s"),
    modified("M", "edit", "modified %s"),
    addedAndModified("AM", "edit", "added and modified %s"),
    deleted("D", "delete", "deleted %s"),
    renamed("R", "move/add", "renamed %s to %s"),
    renamedAndModified("RM", "noPerforceVersion", "renamed %s to %s, also modified"),
    deletedAfterRename("noGitVersion", "move/delete", "deleted %s after being renamed"),
    copied("C", "add", "copied %s to %s");

    private static List<FileChangeType> editChangeTypes = Arrays.asList(modified, renamed, renamedAndModified, copied);

    private String gitValue;

    private String perforceValue;

    private String description;

    FileChangeType(String gitValue, String perforceValue, String description) {
        this.gitValue = gitValue;
        this.perforceValue = perforceValue;
        this.description = description;
    }

    public String getGitValue() {
        return gitValue;
    }

    public String getPerforceValue() {
        return perforceValue;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isEditChangeType(FileChangeType changeType) {
        return editChangeTypes.contains(changeType);
    }

    public static String allValuesAsGitPattern() {
        String pattern = "[";

        for (FileChangeType fileChangeType : FileChangeType.values()) {
            pattern += fileChangeType.getGitValue();
        }
        pattern += "]";
        return pattern;
    }

    public static FileChangeType changeTypeFromGitValue(String gitValue) {
        for (FileChangeType fileChangeType : FileChangeType.values()) {
            if (fileChangeType.getGitValue().equals(gitValue)) {
                return fileChangeType;
            }
        }
        throw new RuntimeException("Failed to find change type for git value " + gitValue);
    }

    public static FileChangeType changeTypeFromPerforceValue(String perforceValue) {
        for (FileChangeType fileChangeType : FileChangeType.values()) {
            if (fileChangeType.getPerforceValue().equals(perforceValue)) {
                return fileChangeType;
            }
        }
        throw new RuntimeException("Failed to find change type for perforce value " + perforceValue);
    }
}

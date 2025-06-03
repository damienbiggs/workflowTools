package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.github.Github;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.util.logging.LogLevel;

import java.util.Optional;

/**
 * Common functionality for actions that amend a commit.
 */
public abstract class BaseCommitAmendAction extends BaseCommitCreateAction {
    protected static final boolean INCLUDE_ALL_CHANGES = true;
    protected static final boolean DONT_INCLUDE_ALL_CHANGES = false;
    protected static final boolean INCLUDE_JOB_RESULTS = true;
    protected static final boolean EXCLUDE_JOB_RESULTS = false;

    private final boolean includeAllChangesInCommit;

    public BaseCommitAmendAction(WorkflowConfig config, boolean includeAllChangesInCommit) {
        super(config);
        this.includeAllChangesInCommit = includeAllChangesInCommit;
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        Optional<String> commitHasChangesReason = commitHasChanges();
        commitHasChangesReason.ifPresent(reason -> log.debug("Amending commit as {}", reason));
        super.skipActionIfTrue(!commitHasChangesReason.isPresent(), "no changes detected");
    }

    @Override
    public void process() {
        if (commitConfig.preferPullRequest && draft.getGithubPullRequest() != null) {
            Github github = serviceLocator.getGithub();
            PullRequest pullRequest = draft.getGithubPullRequest();
            PullRequestForUpdate pullRequestForUpdate = pullRequest.pullRequestForUpdate();
            String targetBranch = determineTargetMergeBranch();
            if (!targetBranch.equals(pullRequest.base.ref)) {
                pullRequestForUpdate.head = targetBranch;
            }
            log.info("Updating pull request {} since --prefer-pull-request is set to true", pullRequest.htmlUrl);
            pullRequestForUpdate.title = draft.summary;
            pullRequestForUpdate.body = draft.toText(commitConfig, false, commitConfig.includeJobResults);
            PullRequest updatedPullRequest = github.updatePullRequest(pullRequestForUpdate);
            draft.setGithubPullRequest(updatedPullRequest);
        }
        super.process();
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommit(updatedCommitText(commitConfig.includeJobResults), includeAllChangesInCommit, gitRepoConfig.noVerify);
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, LogLevel.INFO);
    }

    private Optional<String> commitHasChanges() {
        if (commitTextHasChanges(commitConfig.includeJobResults)) {
            return Optional.of("commit text has changes");
        }

        if (!git.workingDirectoryIsInGitRepo()) {
            return Optional.empty();
        }

        if (git.getAllChanges().isEmpty()) {
            return Optional.empty();
        }

        if (!git.getStagedChanges().isEmpty()) {
            return Optional.of("has staged changes");
        } else if (includeAllChangesInCommit) {
            return Optional.of("has changes");
        } else {
            return Optional.empty();
        }
    }
}

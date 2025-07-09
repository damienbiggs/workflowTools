package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.github.Github;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.util.List;
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

            log.debug("Updating pull request {}", pullRequest.url);
            if (!draft.matchesReviewers(pullRequest.reviewers())) {
                List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
                log.info("Existing pull request reviewers: {}", pullRequest.reviewers());
                log.info("Updating pull request reviewers for {} to {}", pullRequest.url, usernames);
                github.updateReviewersForPullRequest(pullRequest, usernames);
            } else {
                log.debug("Reviewers are the same, not updating");
            }

            String bodyText = draft.toText(commitConfig, false, commitConfig.includeJobResults, false);
            if (!pullRequest.title.equals(draft.summary) || !pullRequest.body.equals(bodyText)) {
                log.info("Updating pull request details for {}", pullRequest.url);
                pullRequest.title = draft.summary;
                pullRequest.body = bodyText;
                github.updatePullRequestDetails(pullRequest);
            } else {
                log.debug("Details are the same, not updating");
            }
        } else {
            super.process();
        }
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommit(updatedCommitText(commitConfig.includeJobResults, true), includeAllChangesInCommit, gitRepoConfig.noVerify);
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

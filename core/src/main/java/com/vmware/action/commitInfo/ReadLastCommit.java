package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.logging.Padder;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@ActionDescription("This MUST be used first to parse the last commit if intending to edit anything in the last commit.")
public class ReadLastCommit extends BaseCommitAction {

    public ReadLastCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        failIfGitRepoOrPerforceClientCannotBeUsed();
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (draft.getGithubPullRequest() != null && commitConfig.preferPullRequest) {
            skipActionDueTo("draft is already associated with pull request " + draft.getGithubPullRequest().number);
        }
    }

    @Override
    public void process() {
        Optional<PullRequest> matchingPullRequest = getMatchingPullRequestIfNeeded();
        if (matchingPullRequest.isPresent()) {
            PullRequest request = matchingPullRequest.get();
            log.info("Reading commit details from pull request {}", request.url);
            draft.fillValuesFromCommitText(request.asText(), commitConfig);
            if (request.reviewRequests != null && request.reviewRequests.nodes != null) {
                draft.reviewedBy = Arrays.stream(request.reviewRequests.nodes).map(node -> node.requestedReviewer.username()).collect(Collectors.joining(","));
            }
            draft.setGithubPullRequest(request);
        } else {
            String commitText = readLastChange();
            if (git.workingDirectoryIsInGitRepo()) {
                draft.branch = gitRepoConfig.determineBranchName();
            }
            draft.fillValuesFromCommitText(commitText, commitConfig);
            if (git.workingDirectoryIsInGitRepo()) {
                log.info("Read last commit from branch {}", draft.branch);
            } else {
                log.info("Read pending changelist {}", draft.perforceChangelistId);
            }
        }

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toText(commitConfig));
        titlePadder.debugTitle();
    }

    private Optional<PullRequest> getMatchingPullRequestIfNeeded() {
        if (!commitConfig.preferPullRequest) {
            return Optional.empty();
        }
        String sourceMergeBranch = determineSourceMergeBranch();
        log.debug("Checking pull requests for request matching source branch {}", sourceMergeBranch);

        return serviceLocator.getGithub().getPullRequestForSourceBranch(githubConfig.githubRepoOwnerName,
                githubConfig.githubRepoName, sourceMergeBranch);
    }


}

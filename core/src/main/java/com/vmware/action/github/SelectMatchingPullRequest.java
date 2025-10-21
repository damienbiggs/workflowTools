package com.vmware.action.github;

import com.vmware.action.base.BaseCommitUsingGithubAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

import java.util.Optional;

@ActionDescription("Selects the matching pull request in Github by merge branch.")
public class SelectMatchingPullRequest extends BaseCommitUsingGithubAction {
    public SelectMatchingPullRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (draft.getGithubPullRequest() != null) {
            skipActionDueTo("already associated with pull request " + draft.getGithubPullRequest().number);
        }
    }

    @Override
    public void process() {
        String sourceMergeBranch = determineSourceMergeBranch();
        log.debug("Checking pull requests for request matching source branch {}", sourceMergeBranch);

        Optional<PullRequest> matchingRequest = github.getPullRequestForSourceBranch(githubConfig.githubRepoOwnerName,
                githubConfig.githubRepoName, sourceMergeBranch, PullRequest.PullRequestState.OPEN);
        if (matchingRequest.isPresent()) {
            log.info("Found matching open pull request {}", matchingRequest.get().url);
            draft.setGithubPullRequest(matchingRequest.get());
        } else if (githubConfig.useAnyMatchingPullRequest) {
            matchingRequest = github.getPullRequestForSourceBranch(githubConfig.githubRepoOwnerName,
                    githubConfig.githubRepoName, sourceMergeBranch, null);

            if (matchingRequest.isPresent()) {
                log.info("Found matching {} pull request {}", matchingRequest.get().state, matchingRequest.get().url);
                draft.setGithubPullRequest(matchingRequest.get());
            }
        }

        if (!matchingRequest.isPresent()) {
            if (gitRepoConfig.failIfNoRequestFound) {
                cancelWithMessage("no matching pull request was found for source branch {}", sourceMergeBranch);
            } else {
                log.info("Failed to find matching pull request for source branch {}", sourceMergeBranch);
            }
        }
    }
}

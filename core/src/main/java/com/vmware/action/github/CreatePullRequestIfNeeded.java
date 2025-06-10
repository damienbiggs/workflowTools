package com.vmware.action.github;

import com.vmware.action.base.BaseCommitUsingGithubAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.User;
import com.vmware.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Creates a pull request in github, uses pull request branch format unless one specified.")
public class CreatePullRequestIfNeeded extends BaseCommitUsingGithubAction {
    public CreatePullRequestIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.hasMergeOrPullRequest(), "pull request with url " + draft.requestUrl + " has already been created");
    }

    @Override
    public void process() {
        PullRequest pullRequest = new PullRequest();
        pullRequest.title = draft.summary;
        pullRequest.body = draft.toText(commitConfig, false, false);
        pullRequest.headRefName = determineSourceMergeBranch();
        pullRequest.baseRefName = determineTargetMergeBranch();
        pullRequest.draft = gitRepoConfig.markAsDraft;

        log.info("Creating pull request with source branch {} and target branch {}", pullRequest.headRefName, pullRequest.baseRefName);
        PullRequest createdRequest = github.createPullRequest(githubConfig.githubRepoOwnerName, githubConfig.githubRepoName, pullRequest);
        draft.setGithubPullRequest(createdRequest);
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as pull request is already associated with a reviewboard review");
        } else if (draft.hasReviewers()) {
            List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
            List<User> users = usernames.stream().map(user -> github.getUser(user)).collect(Collectors.toList());
            github.updateReviewersForPullRequest(createdRequest, users);
        }
        log.info("Created pull request {}", createdRequest.url);
    }
}

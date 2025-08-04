package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import java.util.List;
import java.util.Optional;

@ActionDescription("Sets the git commit details from the associated pull request request.")
public class SetCommitDetailsFromPullRequest extends BaseCommitWithPullRequestAction {
    private ReviewBoard reviewBoard;

    public SetCommitDetailsFromPullRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        if (StringUtils.isEmpty(pullRequest.title) && StringUtils.isEmpty(pullRequest.body)) {
            throw new FatalException("Title and body are blank for pull request {}", pullRequest.number);
        }
        log.info("Using pull request {} ({}) for commit details", pullRequest.number, pullRequest.title);
        String fullText = pullRequest.title + "\n" + pullRequest.body;
        ReviewRequestDraft parsedDraft = new ReviewRequestDraft(fullText, commitConfig);
        draft.summary = parsedDraft.summary;
        draft.description = parsedDraft.description;
        draft.testingDone = parsedDraft.testingDone;
        syncJobBuilds(parsedDraft);
        if (StringUtils.isEmpty(parsedDraft.bugNumbers)) {
            draft.bugNumbers = commitConfig.noBugNumberLabel;
        } else {
            draft.bugNumbers = parsedDraft.bugNumbers;
        }
        draft.reviewedBy = pullRequest.reviewers();
        draft.codeOwners = pullRequest.codeOwners();
    }

    private void syncJobBuilds(ReviewRequestDraft draftConstructedFromReviewDetails) {
        List<JobBuild> existingBuilds = draft.jobBuilds;
        List<JobBuild> buildsFromReview = draftConstructedFromReviewDetails.jobBuilds;
        for (JobBuild jobBuildFromReview : buildsFromReview) {
            Optional<JobBuild> existingBuild =
                    existingBuilds.stream().filter(build -> build.url.equals(jobBuildFromReview.url)).findFirst();
            if (!existingBuild.isPresent()) {
                existingBuilds.add(jobBuildFromReview);
            } else if (jobBuildFromReview.status != null) {
                existingBuild.get().status = jobBuildFromReview.status;
            }
        }
        existingBuilds.removeIf(build -> buildsFromReview.stream()
                .noneMatch(buildFromReview -> StringUtils.equals(build.url, buildFromReview.url)));
    }
}
package com.vmware.github.domain;

public enum MutationName {
    createPullRequest,
    updatePullRequest,
    markPullRequestReadyForReview,
    convertPullRequestToDraft,
    updatePullRequestBranch,
    mergePullRequest,
    closePullRequest,
    requestReviews;
}

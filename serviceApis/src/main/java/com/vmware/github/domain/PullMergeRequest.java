package com.vmware.github.domain;

public class PullMergeRequest {
    public String pullRequestId;
    public String commitHeadline;
    public String commitBody;
    public String expectedHeadOid;
    public MergeMethod mergeMethod;
    public String updateMethod;

    public enum MergeMethod {
        SQUASH,
        MERGE,
        REBASE
    }
}

package com.vmware.github.domain;

public class ReviewThread {
    public String id;
    public String path;
    public boolean isResolved;
    public ReviewCommentNodes comments;

    public static class ReviewCommentNodes {
        public ReviewComment[] nodes;
    }

}

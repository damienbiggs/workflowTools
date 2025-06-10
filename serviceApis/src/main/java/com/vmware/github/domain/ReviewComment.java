package com.vmware.github.domain;

import java.util.Date;

public class ReviewComment {
    public String id;
    public Date createdAt;
    public String body;
    public String diffHunk;
    public User author;

    public Date getCreatedAt() {
        return createdAt;
    }
}

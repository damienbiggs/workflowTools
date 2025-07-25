package com.vmware.github.domain;

import java.util.Collection;

public class RequestedReviewers {
    public String pullRequestId;
    public String[] userIds;
    public String[] teamIds;
    public Boolean union = false;

    public RequestedReviewers() {
    }

    public RequestedReviewers(String pullRequestId, Collection<User> users) {
        this.pullRequestId = pullRequestId;
        this.userIds = users.stream().filter(User::isUser).map(user -> user.id).toArray(String[]::new);
        this.teamIds = users.stream().filter(User::isTeam).map(user -> user.id).toArray(String[]::new);
    }
}

package com.vmware.github.domain;

import com.vmware.AutocompleteUser;

public class User implements AutocompleteUser {
    public String id;
    public String login;
    public String name;
    public String slug;
    public String combinedSlug;
    public String url;
    public String company;
    public String location;
    public Organization organization;

    public User() {
    }

    public User(String login) {
        this.login = login;
    }

    public User(String login, String name) {
        this.login = login;
        this.name = name;
    }

    @Override
    public String username() {
        return isUser() ? login : "@" + slug;
    }

    @Override
    public String fullName() {
        return name;
    }

    public boolean isUser() {
        return slug == null;
    }

    public boolean isTeam() {
        return slug != null;
    }
}

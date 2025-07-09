package com.vmware.github.domain;

public class Organization {
    public String login;
    public String name;
    public TeamsNode teams;

    public static class TeamsNode {
        public User[] nodes;
    }
}

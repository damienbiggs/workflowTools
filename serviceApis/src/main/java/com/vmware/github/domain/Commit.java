package com.vmware.github.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Commit {
    public String oid;

    @SerializedName("statusCheckRollup")
    public StatusCheckRollup statusCheckRollup;

    public class StatusCheckRollup {
        public Status status;
        public Contexts contexts;
    }

    public class Contexts {
        @SerializedName("checkRunCount")
        public int checkRunCount;
        @SerializedName("statusContextCount")
        public int statusContextCount;
        @SerializedName("totalCount")
        public int totalCount;
        public StatusNode[] nodes;
    }

    public class StatusNode {
        public String id;
        public String databaseId;
        public String name;
        public String summary;
        public Status status;
        public Date createdAt;
        public Date completedAt;
        public String targetUrl;
        public CheckSuite checkSuite;


        public String fullName() {
            if (checkSuite != null && checkSuite.workflowRun != null && checkSuite.workflowRun.workflow != null) {
                return checkSuite.workflowRun.workflow.name + " / " + name;
            } else {
                return name;
            }
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof StatusNode) {
                StatusNode otherNode = (StatusNode) obj;
                return fullName().equals(otherNode.fullName());
            } else {
                return false;
            }
        }
    }

    public static class CheckSuite {
        public WorkflowRun workflowRun;
    }

    public static class WorkflowRun {
        public Workflow workflow;
    }

    public static class Workflow {
        public String name;
    }

    public enum Status {
        SUCCESS,
        FAILURE,
        PENDING
    }
}

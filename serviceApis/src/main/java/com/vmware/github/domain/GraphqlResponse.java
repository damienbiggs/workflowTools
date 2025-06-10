package com.vmware.github.domain;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.vmware.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphqlResponse {

    public enum PullRequestReviewDecision {
        APPROVED, CHANGES_REQUEST, REVIEW_REQUIRED
    }

    @JsonAdapter(ResponseDataDeserializer.class)
    public ResponseData data;
    public ErrorMessage[] errors;

    public static class ResponseData {
        public Repository repository;
        public MutatedPullRequest mutatedPullRequest;
        public Search search;
        public User user;
    }

    public static class Search {
        public int userCount;
        public UserNode[] edges;

        public List<User> usersForCompany(String companyName) {
            return Arrays.stream(edges).map(edge -> edge.node).filter(user -> userBelongsToCompany(user, companyName)).collect(Collectors.toList());
        }

        private boolean userBelongsToCompany(User user, String companyName) {
            if (StringUtils.isEmpty(companyName)) {
                return true;
            } else {
                return user.organization != null && companyName.equalsIgnoreCase(user.organization.login);
            }
        }
    }

    public static class Repository {
        public String id;
        public PullRequest pullRequest;
        public PullRequestsNode pullRequests;

    }

    public static class PullRequestsNode {
        public int totalCount;
        public PullRequest[] nodes;
    }

    public static class UserNode {
        public User node;
    }

    public static class MutatedPullRequest {
        public PullRequest pullRequest;
    }

    public static class ErrorMessage {
        public String message;
        public ErrorLocation[] locations;


        @Override
        public String toString() {
            return message + " " + Arrays.toString(locations);
        }
    }

    public static class ErrorLocation {
        public int line;
        public int column;

        @Override
        public String toString() {
            return "line " + line + " column " + column;
        }
    }

    public static class ResponseDataDeserializer implements JsonDeserializer<ResponseData> {

        @Override
        public ResponseData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject dataObject = jsonElement.getAsJsonObject();
            ResponseData data = new ResponseData();
            Optional<JsonElement> mutationResult = Arrays.stream(MutationName.values())
                    .filter(value -> dataObject.has(value.name()))
                    .map(value -> dataObject.get(value.name())).findFirst();
            mutationResult.ifPresent(element ->
                    data.mutatedPullRequest = jsonDeserializationContext.deserialize(element, MutatedPullRequest.class));

            data.user = jsonDeserializationContext.deserialize(dataObject.get("user"), User.class);
            data.repository = jsonDeserializationContext.deserialize(dataObject.get("repository"), Repository.class);
            data.search = jsonDeserializationContext.deserialize(dataObject.get("search"), Search.class);
            return data;
        }
    }
}

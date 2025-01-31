package com.vmware.chrome.domain;

import java.util.Collections;
import java.util.Map;

public class ApiResponse {
    public ApiRequest sourceRequest;

    public int id;

    public String method;

    public Params params;

    public Result result;

    public Error error;

    public Map exceptionDetails;

    public String getType() {
        return (String) getValueMap().get("type");
    }

    public String getDescription() {
        return (String) getValueMap().get("description");
    }

    public String getValue() {
        return (String) getValueMap().get("value");
    }

    public String getData() {
        return result != null ? result.data : null;
    }

    public String getParamsData() {
        return params != null ? params.data : null;
    }

    public String getClassName() {
        return (String) getValueMap().get("className");
    }

    public boolean matchesElementId(String elementId) {
        return elementId != null && getDescription() != null && getDescription().contains("#" + elementId);
    }

    public boolean matchesUrl(String url) {
        return getValue() != null && (getValue().equalsIgnoreCase(url) || getValue().matches(url));
    }

    public boolean hasObjectId() {
        return getValueMap().get("objectId") != null;
    }

    public boolean matchesRequestSource(String source) {
        return sourceRequest.getSource() != null && sourceRequest.getSource().equalsIgnoreCase(source);
    }

    public String getRequestExpression() {
        return sourceRequest.getExpression();
    }

    private Map getValueMap() {
        if (this.result == null || this.result.result == null) {
            return Collections.emptyMap();
        }
        return this.result.result;
    }

    public class Result {
        public Map result;

        private String data;
    }

    public class Error {
        public String code;

        public String message;

        private String data;
    }

    public class Params {

        private String data;
    }

}

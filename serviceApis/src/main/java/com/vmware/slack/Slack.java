package com.vmware.slack;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.BadRequestException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.slack.domain.ChannelMessage;
import com.vmware.slack.domain.SlackResponse;

public class Slack extends AbstractRestService {

    public Slack(String url, String username) {
        super(url, "/api", ApiAuthentication.none, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder().namingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        this.connection.addStatefulParam(RequestHeader.aBearerAuthHeader(""));
    }

    public void sendMessage(String channel, String text) {
        ChannelMessage message = new ChannelMessage();
        message.channel = channel;
        message.text = text;

        SlackResponse response = post(apiUrl + "/chat.postMessage", SlackResponse.class, message);
        checkResponse(response);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {

    }

    @Override
    protected void loginManually() {

    }

    private void checkResponse(SlackResponse response) {
        if (response.ok) {
            return;
        }

        throw new BadRequestException(response.toString());
    }
}

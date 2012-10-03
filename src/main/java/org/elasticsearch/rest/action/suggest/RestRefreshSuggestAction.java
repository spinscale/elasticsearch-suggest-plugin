package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.*;
import static org.elasticsearch.rest.RestStatus.*;

import java.io.IOException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.suggest.SuggestRefreshRequest;
import org.elasticsearch.action.suggest.SuggestRefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestActions;

public class RestRefreshSuggestAction extends BaseRestHandler {

    @Inject public RestRefreshSuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_suggestRefresh", this);
        controller.registerHandler(POST, "/{index}/{type}/_suggestRefresh", this); // TODO: only refresh per index here
        controller.registerHandler(POST, "/{index}/{type}/{field}/_suggestRefresh", this); // TODO: only refresh per index field here
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String[] indices = RestActions.splitIndices(request.param("index"));
        final String field = request.param("field");

        SuggestRefreshRequest suggestRefreshRequest = new SuggestRefreshRequest(indices);
        suggestRefreshRequest.field(field);

        client.execute(SuggestRefreshAction.INSTANCE, suggestRefreshRequest, new ActionListener<SuggestRefreshResponse>() {

            @Override
            public void onResponse(SuggestRefreshResponse response) {
                try {
                    channel.sendResponse(new StringRestResponse(OK));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });

    }

}

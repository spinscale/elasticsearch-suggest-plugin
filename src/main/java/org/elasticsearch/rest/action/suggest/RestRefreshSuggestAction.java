package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.*;
import static org.elasticsearch.rest.RestStatus.*;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.suggest.SuggestRefreshRequest;
import org.elasticsearch.action.suggest.SuggestRefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestActions;

public class RestRefreshSuggestAction extends BaseRestHandler {

    @Inject public RestRefreshSuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_suggestRefresh", this);
        controller.registerHandler(POST, "/{index}/{type}/_suggestRefresh", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String[] indices = RestActions.splitIndices(request.param("index"));

        try {

            SuggestRefreshRequest suggestRefreshRequest = new SuggestRefreshRequest(indices);

            if (request.hasContent()) {
                XContentParser parser = XContentFactory.xContent(request.content()).createParser(request.content());
                Map<String, Object> parserMap = parser.mapAndClose();

                if (parserMap.containsKey("field")) {
                    suggestRefreshRequest.field(XContentMapValues.nodeStringValue(parserMap.get("field"), ""));
                }
            }

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

        } catch (IOException e) {
            try {
                channel.sendResponse(new XContentThrowableRestResponse(request, e));
            } catch (IOException e1) {
                logger.error("Failed to send failure response", e1);
            }
        }
    }

}

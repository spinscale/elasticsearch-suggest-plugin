package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.*;
import static org.elasticsearch.rest.RestStatus.*;
import static org.elasticsearch.rest.action.support.RestActions.*;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

public class RestSuggestAction extends BaseRestHandler {

    @Inject
    public RestSuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/{index}/{type}/_suggest", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String[] indices = RestActions.splitIndices(request.param("index"));

        try {
            XContentParser parser = XContentFactory.xContent(request.content()).createParser(request.content());
            Map<String, Object> parserMap = parser.mapAndClose();

            SuggestRequest suggestRequest = new SuggestRequest(indices);
            suggestRequest.field(XContentMapValues.nodeStringValue(parserMap.get("field"), ""));
            suggestRequest.term(XContentMapValues.nodeStringValue(parserMap.get("term"), ""));
            suggestRequest.similarity(XContentMapValues.nodeFloatValue(parserMap.get("similarity"), 1.0f));
            suggestRequest.size(XContentMapValues.nodeIntegerValue(parserMap.get("size"), 10));

            client.execute(SuggestAction.INSTANCE, suggestRequest, new ActionListener<SuggestResponse>() {
                @Override
                public void onResponse(SuggestResponse response) {
                    try {
                        XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                        builder.startObject();
                        builder.field("suggestions", response.suggestions());
                        buildBroadcastShardsHeader(builder, response);

                        builder.endObject();
                        channel.sendResponse(new XContentRestResponse(request, OK, builder));
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

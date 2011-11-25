package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

import java.io.IOException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.node.NodeClientWithSuggest;
import org.elasticsearch.client.transport.TransportClientNodesService;
import org.elasticsearch.client.transport.support.InternalTransportClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestXContentBuilder;
import org.elasticsearch.service.suggest.SuggestService;

public class SuggestAction extends BaseRestHandler {

    @Inject SuggestService suggestService;
    @Inject TransportClientNodesService nodesService;
    private NodeClientWithSuggest nodeClient;

    @Inject public SuggestAction(Settings settings, InternalTransportClient client, NodeClientWithSuggest nodeClient, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/{index}/{type}/_suggest", this);
        this.nodeClient = nodeClient;
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String[] indices = RestActions.splitIndices(request.param("index"));

        SuggestRequest suggestRequest = new SuggestRequest(indices);
        suggestRequest.query(request.contentByteArray());

        nodeClient.suggest(suggestRequest, new ActionListener<SuggestResponse>() {
            public void onResponse(SuggestResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    System.out.println("GOT GOT GOT: " + response.suggestionsAsString());
                    builder.field("suggestions", response.suggestionsAsString());
                    builder.field("count", response.count());
                    buildBroadcastShardsHeader(builder, response);

                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
                try {
                    e.printStackTrace();
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }

        });
    }
}

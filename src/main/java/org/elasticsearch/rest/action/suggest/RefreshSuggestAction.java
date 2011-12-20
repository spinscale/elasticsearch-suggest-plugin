package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.*;
import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.NodesSuggestRefreshResponse;
import org.elasticsearch.client.node.NodeClientWithSuggest;
import org.elasticsearch.client.transport.support.InternalTransportClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.StringRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;

public class RefreshSuggestAction extends BaseRestHandler {

    private NodeClientWithSuggest nodeClient;

    @Inject public RefreshSuggestAction(Settings settings, InternalTransportClient client,
            NodeClientWithSuggest nodeClient, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_suggestRefresh", this);
        controller.registerHandler(POST, "/{index}/{type}/_suggestRefresh", this); // TODO: only refresh per index here
        this.nodeClient = nodeClient;
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        NodesSuggestRefreshRequest suggestRefreshRequest = new NodesSuggestRefreshRequest();

        nodeClient.suggestRefresh(suggestRefreshRequest, new ActionListener<NodesSuggestRefreshResponse>() {

            public void onResponse(NodesSuggestRefreshResponse response) {
                try {
                    channel.sendResponse(new StringRestResponse(OK));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

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

package org.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.NodesSuggestRefreshResponse;
import org.elasticsearch.client.action.support.BaseRequestBuilder;
import org.elasticsearch.client.node.NodeClientWithSuggest;


public class SuggestRefreshRequestBuilder extends BaseRequestBuilder<NodesSuggestRefreshRequest, NodesSuggestRefreshResponse> {

    private NodeClientWithSuggest nodeClient;

    public SuggestRefreshRequestBuilder(NodeClientWithSuggest client) {
        super(client, new NodesSuggestRefreshRequest());
        nodeClient = client;
    }

    @Override
    protected void doExecute(ActionListener<NodesSuggestRefreshResponse> listener) {
        nodeClient.suggestRefresh(request, listener);
    }

    public SuggestRefreshRequestBuilder setIndices(String ... indices) {
//        request.indices(indices);
        return this;
    }

}

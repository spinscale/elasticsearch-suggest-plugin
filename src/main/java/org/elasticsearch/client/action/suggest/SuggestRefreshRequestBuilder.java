package org.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.NodesSuggestRefreshResponse;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.support.BaseRequestBuilder;
import org.elasticsearch.client.Client;


public class SuggestRefreshRequestBuilder extends BaseRequestBuilder<NodesSuggestRefreshRequest, NodesSuggestRefreshResponse> {

    public SuggestRefreshRequestBuilder(Client client) {
        super(client, new NodesSuggestRefreshRequest());
    }

    @Override
    protected void doExecute(ActionListener<NodesSuggestRefreshResponse> listener) {
        client.execute(SuggestRefreshAction.INSTANCE, request, listener);
    }

    public SuggestRefreshRequestBuilder setIndices(String ... indices) {
//        request.indices(indices);
        return this;
    }

}

package org.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.client.action.support.BaseRequestBuilder;
import org.elasticsearch.client.node.NodeClientWithSuggest;

public class SuggestRequestBuilder extends BaseRequestBuilder<SuggestRequest, SuggestResponse> {

//    private InternalTransportClientWithSuggest transportClient;
    private NodeClientWithSuggest nodeClient;

    public SuggestRequestBuilder(NodeClientWithSuggest client) {
        super(client, new SuggestRequest());
        nodeClient = client;
    }

//    public SuggestRequestBuilder(InternalTransportClientWithSuggest client) {
//        super(client, new SuggestRequest());
//        transportClient = client;
//    }

//    @Inject public SuggestRequestBuilder(InternalTransportClientWithSuggest client, SuggestRequest request) {
//        super(client, request);
//        this.client = client;
//    }

    @Override
    protected void doExecute(ActionListener<SuggestResponse> listener) {
//        if (transportClient != null) {
//            transportClient.suggest(request, listener);
//        } else if (nodeClient != null) {
            nodeClient.suggest(request, listener);
//        }
    }

    public SuggestRequestBuilder setQuery(byte[] querySource) {
        request.query(querySource);
        return this;
    }

    /**
     * Controls the operation threading model.
     */
    public SuggestRequestBuilder setOperationThreading(BroadcastOperationThreading operationThreading) {
        request.operationThreading(operationThreading);
        return this;
    }

    /**
     * Should the listener be called on a separate thread if needed.
     */
    public SuggestRequestBuilder setListenerThreaded(boolean threadedListener) {
        request.listenerThreaded(threadedListener);
        return this;
    }

    public SuggestRequestBuilder setIndices(String ... indices) {
        request.indices(indices);
        return this;
    }
}

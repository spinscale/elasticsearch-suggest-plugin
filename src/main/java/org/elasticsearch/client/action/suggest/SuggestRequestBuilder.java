package org.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.support.BaseRequestBuilder;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.client.Client;

public class SuggestRequestBuilder extends BaseRequestBuilder<SuggestRequest, SuggestResponse> {

    public SuggestRequestBuilder(Client client) {
        super(client, new SuggestRequest());
    }

    @Override
    protected void doExecute(ActionListener<SuggestResponse> listener) {
        client.execute(SuggestAction.INSTANCE, request, listener);
    }

    public SuggestRequestBuilder term(String term) {
        request.term(term);
        return this;
    }

    public SuggestRequestBuilder field(String field) {
        request.field(field);
        return this;
    }

    public SuggestRequestBuilder similarity(float similarity) {
        request.similarity(similarity);
        return this;
    }

    public SuggestRequestBuilder size(int size) {
        request.size(size);
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

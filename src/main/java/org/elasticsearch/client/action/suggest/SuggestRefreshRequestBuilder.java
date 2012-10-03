package org.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.suggest.SuggestRefreshRequest;
import org.elasticsearch.action.suggest.SuggestRefreshResponse;
import org.elasticsearch.action.support.BaseRequestBuilder;
import org.elasticsearch.client.Client;


public class SuggestRefreshRequestBuilder extends BaseRequestBuilder<SuggestRefreshRequest, SuggestRefreshResponse> {

    public SuggestRefreshRequestBuilder(Client client) {
        super(client, new SuggestRefreshRequest());
    }

    public SuggestRefreshRequestBuilder setIndices(String ... indices) {
        request.indices(indices);
        return this;
    }

    public SuggestRefreshRequestBuilder setField(String field) {
        request.field(field);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<SuggestRefreshResponse> listener) {
        client.execute(SuggestRefreshAction.INSTANCE, request, listener);
    }

}

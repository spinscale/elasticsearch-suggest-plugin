package de.spinscale.elasticsearch.client.action.suggest;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import de.spinscale.elasticsearch.action.suggest.SuggestRefreshAction;
import de.spinscale.elasticsearch.action.suggest.SuggestRefreshRequest;
import de.spinscale.elasticsearch.action.suggest.SuggestRefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;


public class SuggestRefreshRequestBuilder extends ActionRequestBuilder<SuggestRefreshRequest, SuggestRefreshResponse, SuggestRefreshRequestBuilder> {

    public SuggestRefreshRequestBuilder(Client client) {
        super((InternalClient) client, new SuggestRefreshRequest());
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
        ((Client)client).execute(SuggestRefreshAction.INSTANCE, request, listener);
    }

}

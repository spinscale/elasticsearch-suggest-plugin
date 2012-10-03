package org.elasticsearch.action.suggest;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;

public class SuggestRefreshAction extends Action<SuggestRefreshRequest, SuggestRefreshResponse, SuggestRefreshRequestBuilder> {

    public static final SuggestRefreshAction INSTANCE = new SuggestRefreshAction();
    public static final String NAME = "suggestRefresh";

    private SuggestRefreshAction() {
        super(NAME);
    }

    @Override
    public SuggestRefreshRequestBuilder newRequestBuilder(Client client) {
        return new SuggestRefreshRequestBuilder(client);
    }

    @Override
    public SuggestRefreshResponse newResponse() {
        return new SuggestRefreshResponse();
    }

}

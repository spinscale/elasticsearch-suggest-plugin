package org.elasticsearch.action.suggest;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.suggest.SuggestRequestBuilder;

public class SuggestAction extends Action<SuggestRequest, SuggestResponse, SuggestRequestBuilder> {

    public static final SuggestAction INSTANCE = new SuggestAction();
    public static final String NAME = "suggest";

    private SuggestAction() {
        super(NAME);
    }

    @Override
    public SuggestResponse newResponse() {
        return new SuggestResponse();
    }

    @Override
    public SuggestRequestBuilder newRequestBuilder(Client client) {
        return new SuggestRequestBuilder(client);
    }

}

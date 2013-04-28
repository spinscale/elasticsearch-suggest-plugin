package de.spinscale.elasticsearch.client.action.suggest;

import de.spinscale.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import de.spinscale.elasticsearch.action.suggest.SuggestRequest;
import de.spinscale.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;

public class SuggestRequestBuilder extends ActionRequestBuilder<SuggestRequest, SuggestResponse, SuggestRequestBuilder> {

    public SuggestRequestBuilder(Client client) {
        super((InternalClient) client, new SuggestRequest());
    }

    @Override
    protected void doExecute(ActionListener<SuggestResponse> listener) {
        ((Client)client).execute(SuggestAction.INSTANCE, request, listener);
    }

    public SuggestRequestBuilder term(String term) {
        request.term(term);
        return this;
    }

    public SuggestRequestBuilder field(String field) {
        request.field(field);
        return this;
    }

    public SuggestRequestBuilder suggestType(String suggestType) {
        request.suggestType(suggestType);
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

    public SuggestRequestBuilder setIndices(String ... indices) {
        request.indices(indices);
        return this;
    }

    public SuggestRequestBuilder queryAnalyzer(String queryAnalyzer) {
        request.queryAnalyzer(queryAnalyzer);
        return this;
    }

    public SuggestRequestBuilder indexAnalyzer(String indexAnalyzer) {
        request.indexAnalyzer(indexAnalyzer);
        return this;
    }
}

package de.spinscale.elasticsearch.client.action.suggest;

import de.spinscale.elasticsearch.action.suggest.suggest.SuggestAction;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import de.spinscale.elasticsearch.action.suggest.suggest.SuggestRequest;
import de.spinscale.elasticsearch.action.suggest.suggest.SuggestResponse;
import org.elasticsearch.client.Client;

public class SuggestRequestBuilder extends ActionRequestBuilder<SuggestRequest, SuggestResponse, SuggestRequestBuilder, Client> {

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

    public SuggestRequestBuilder analyzer(String analyzer) {
        request.indexAnalyzer(analyzer);
        request.queryAnalyzer(analyzer);
        return this;
    }

    public SuggestRequestBuilder preservePositionIncrements(boolean preservePositionIncrements) {
        request.preservePositionIncrements(preservePositionIncrements);
        return this;
    }
}

package org.elasticsearch.module.suggest.test;

import java.util.List;

import org.elasticsearch.action.suggest.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class TransportSuggestActionTest extends AbstractSuggestTest {

    public TransportSuggestActionTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size, Float similarity) throws Exception {
        SuggestRequest request = new SuggestRequest(index);

        request.term(term);
        request.field(field);

        if (size != null) {
            request.size(size);
        }
        if (similarity != null && similarity > 0.0 && similarity < 1.0) {
            request.similarity(similarity);
        }

        SuggestResponse response = node.client().execute(SuggestAction.INSTANCE, request).actionGet();

        return response.suggestions();
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size)
            throws Exception {
        return getSuggestions(index, field, term, size, null);
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        SuggestRefreshRequest refreshRequest = new SuggestRefreshRequest();
        node.client().execute(SuggestRefreshAction.INSTANCE, refreshRequest).actionGet();
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        SuggestRefreshRequest refreshRequest = new SuggestRefreshRequest(index);
        node.client().execute(SuggestRefreshAction.INSTANCE, refreshRequest).actionGet();
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        SuggestRefreshRequest refreshRequest = new SuggestRefreshRequest(index);
        refreshRequest.field(field);
        node.client().execute(SuggestRefreshAction.INSTANCE, refreshRequest).actionGet();
    }

}

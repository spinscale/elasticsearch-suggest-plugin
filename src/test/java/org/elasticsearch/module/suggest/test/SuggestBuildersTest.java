package org.elasticsearch.module.suggest.test;

import java.util.List;

import org.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;
import org.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class SuggestBuildersTest extends AbstractSuggestTest {

    public SuggestBuildersTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size, Float similarity) throws Exception {
        return getBuilder(index, field, term, size).similarity(similarity).execute().actionGet().getSuggestions();
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size) throws Exception {
        return getBuilder(index, field, term, size).execute().actionGet().getSuggestions();
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(node.client());
        builder.execute().actionGet();
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(node.client()).setIndices(index);
        builder.execute().actionGet();
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(node.client()).setIndices(index).setField(field);
        builder.execute().actionGet();
    }

    private SuggestRequestBuilder getBuilder(String index, String field, String term, Integer size) throws Exception {
        return new SuggestRequestBuilder(node.client())
            .setIndices(index)
            .field(field)
            .term(term)
            .size(size);
    }
}

package org.elasticsearch.rest.action.suggest.SuggestActionTest;

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
    public List<String> getSuggestions(String field, String term, Integer size, Float similarity) throws Exception {
        return getBuilder(field, term, size).similarity(similarity).execute().actionGet().getSuggestions();
    }

    @Override
    public List<String> getSuggestions(String field, String term, Integer size) throws Exception {
        return getBuilder(field, term, size).execute().actionGet().getSuggestions();
    }

    @Override
    public void refreshSuggestIndex() throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(node.client());
        builder.execute().actionGet();
    }

    private SuggestRequestBuilder getBuilder(String field, String term, Integer size) throws Exception {
        return new SuggestRequestBuilder(node.client())
            .field(field)
            .term(term)
            .size(size);
    }
}

package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import java.util.List;

import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.suggest.TransportNodesSuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.node.internal.InternalNode;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class TransportSuggestActionTest extends AbstractSuggestTest {

    public TransportSuggestActionTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    @Override
    public List<String> getSuggestions(String field, String term, Integer size, Float similarity) throws Exception {
        TransportSuggestAction suggestAction = ((InternalNode) node).injector().getInstance(TransportSuggestAction.class);

        SuggestRequest request = new SuggestRequest("products");
        request.term(term);
        request.field(field);

        if (size != null) {
            request.size(size);
        }
        if (similarity != null && similarity > 0.0 && similarity < 1.0) {
            request.similarity(similarity);
        }

        SuggestResponse response = suggestAction.execute(request).actionGet();

        return response.suggestions();
    }

    @Override
    public List<String> getSuggestions(String field, String term, Integer size)
            throws Exception {
        return getSuggestions(field, term, size, null);
    }

    @Override
    public void refreshSuggestIndex() throws Exception {
        TransportNodesSuggestRefreshAction refreshAction = ((InternalNode) node).injector().getInstance(TransportNodesSuggestRefreshAction.class);
        NodesSuggestRefreshRequest refreshRequest = new NodesSuggestRefreshRequest();
        refreshAction.execute(refreshRequest).actionGet();
    }

}

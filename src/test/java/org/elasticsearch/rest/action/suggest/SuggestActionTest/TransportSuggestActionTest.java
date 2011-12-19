package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.common.collect.Maps;
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

        Map<String, Object> querySource = Maps.newHashMap();
        querySource.put("term", term);
        querySource.put("field", field);
        if (size != null) {
            querySource.put("size", size);
        }
        if (similarity != null && similarity > 0.0 && similarity < 1.0) {
            querySource.put("similarity", similarity);
        }

        SuggestRequest request = new SuggestRequest("products").query(querySource);
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
        // TODO: dont sleep, but do something instead
        Thread.sleep(2000);
    }

}

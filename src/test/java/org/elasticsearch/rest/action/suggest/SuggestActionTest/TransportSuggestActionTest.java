package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import static org.elasticsearch.rest.action.suggest.SuggestActionTest.NodeTestHelper.*;
import static org.elasticsearch.rest.action.suggest.SuggestActionTest.ProductTestHelper.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;

import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.junit.Before;
import org.junit.Test;

public class TransportSuggestActionTest {

    private Node node;
    private TransportSuggestAction suggestAction;
    private String clusterName = "TransportSuggestActionTestCluster_" + Math.random();

    @Before
    public void setup() throws Exception {
        node = createNode(clusterName, 1);
        suggestAction = ((InternalNode) node).injector().getInstance(TransportSuggestAction.class);
    }

    @Test
    public void testThatSuggestWorks() throws Exception {
        Map<String, String> querySource = Maps.newHashMap();
        querySource.put("size", "10");
        querySource.put("term", "a");
        querySource.put("field", "foo");

        SuggestRequest request = new SuggestRequest("products").query(querySource);
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("foo", "ab");
        products.get(1).put("foo", "abc");
        products.get(2).put("foo", "abcd");
        indexProducts(products, node);
        SuggestResponse response = suggestAction.execute(request).actionGet();

        assertThat(response.suggestions(), contains("ab", "abc", "abcd"));

        products = createProducts(1);
        products.get(0).put("foo", "abcde");
        indexProducts(products, node);
        Thread.sleep(2000);
        response = suggestAction.execute(request).actionGet();
        assertThat(response.suggestions(), contains("ab", "abc", "abcd", "abcde"));
    }
}

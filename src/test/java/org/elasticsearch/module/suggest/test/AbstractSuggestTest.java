package org.elasticsearch.module.suggest.test;

import static org.elasticsearch.module.suggest.test.NodeTestHelper.*;
import static org.elasticsearch.module.suggest.test.ProductTestHelper.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public abstract class AbstractSuggestTest {

    protected final String clusterName = "SuggestTest_" + Math.random();
    protected Node node;
    protected List<Node> nodes = Lists.newArrayList();
    public static final String DEFAULT_INDEX = "products";

    @Parameters
    public static Collection<Object[]> data() {
        // first argument: number of shards, second argument: number of nodes
//        Object[][] data = new Object[][] { { 1,1 } };
        Object[][] data = new Object[][] { { 1, 1 }, { 4, 1 }, { 10, 1 }, { 4, 4 } };
        return Arrays.asList(data);
    }

    public AbstractSuggestTest(int shards, int nodeCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(nodeCount);
        List<Future<Node>> nodeFutures = Lists.newArrayList();

        for (int i = 0 ; i < nodeCount ; i++) {
            Future<Node> nodeFuture = executor.submit(createNode(clusterName, shards));
            nodeFutures.add(nodeFuture);
        }

        for (Future<Node> nodeFuture : nodeFutures) {
            nodes.add(nodeFuture.get());
        }

        node = nodes.get(0);
        createIndexWithMapping("products", node);
    }


    @After
    public void stopNodes() throws Exception {
        for (Node nodeToStop : nodes) {
            nodeToStop.client().close();
            nodeToStop.close();
        }
    }

    abstract public List<String> getSuggestions(String index, String field, String term, Integer size, Float similarity) throws Exception;
    abstract public List<String> getSuggestions(String index, String field, String term, Integer size) throws Exception;
    abstract public void refreshAllSuggesters() throws Exception;
    abstract public void refreshIndexSuggesters(String index) throws Exception;
    abstract public void refreshFieldSuggesters(String index, String field) throws Exception;

    @Test
    public void testThatSimpleSuggestionWorks() throws Exception {
        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foobar");
        products.get(3).put("ProductName", "boof");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertThat(suggestions.toString(), suggestions, hasSize(3));
        assertThat(suggestions.toString(), suggestions, contains("foo", "foob", "foobar"));
    }

    @Test
    public void testThatAllFieldSuggestionsWorks() throws Exception {
        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foobar");
        products.get(3).put("ProductName", "boof");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("_all", "foo", 10);
        assertThat(suggestions.toString(), suggestions, hasSize(3));
        assertThat(suggestions.toString(), suggestions, contains("foo", "foob", "foobar"));
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimit() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "foob");
        products.get(1).put("ProductName", "fooba");
        products.get(2).put("ProductName", "foobar");
        assertThat(products.size(), is(3));
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertThat(suggestions + " is not correct", suggestions, hasSize(2));
        assertThat(suggestions.toString(), suggestions, contains("foob", "fooba"));
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimitWithConcreteWord() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "fooba");
        products.get(2).put("ProductName", "foobar");
        assertThat(products.size(), is(3));
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertThat(suggestions + " is not correct", suggestions, hasSize(2));
        assertThat(suggestions, contains("foo", "fooba"));
    }

    @Test
    public void testThatSuggestionShouldNotContainDuplicates() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foo");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions, contains("foo", "foob"));
    }

    @Test
    public void testThatSuggestionShouldWorkOnDifferentFields() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "Kochjacke Pute");
        products.get(1).put("ProductName", "Kochjacke Henne");
        products.get(2).put("ProductName", "Kochjacke Hahn");
        products.get(0).put("Description", "Kochjacke Pute");
        products.get(1).put("Description", "Kochjacke Henne");
        products.get(2).put("Description", "Kochjacke Hahn");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochjacke", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke hahn", "kochjacke henne", "kochjacke pute");

        suggestions = getSuggestions("Description", "Kochjacke", 10);
        assertSuggestions(suggestions, "Kochjacke Hahn", "Kochjacke Henne", "Kochjacke Pute");
    }

    @Test
    public void testThatSuggestionShouldWorkWithWhitespaces() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "Kochjacke Paul");
        products.get(1).put("ProductName", "Kochjacke Pauline");
        products.get(2).put("ProductName", "Kochjacke Paulinea");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochja", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke paul", "kochjacke pauline", "kochjacke paulinea");

        suggestions = getSuggestions("ProductName.suggest", "kochjacke ", 10);
        assertSuggestions(suggestions, "kochjacke paul", "kochjacke pauline", "kochjacke paulinea");

        suggestions = getSuggestions("ProductName.suggest", "kochjacke pauline", 10);
        assertSuggestions(suggestions, "kochjacke pauline", "kochjacke paulinea");
    }

    @Test
    public void testThatSuggestionWithShingleWorksAfterUpdate() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "Kochjacke Paul");
        products.get(1).put("ProductName", "Kochjacke Pauline");
        products.get(2).put("ProductName", "Kochjacke Paulinator");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochjacke", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke paul", "kochjacke paulinator", "kochjacke pauline");

        products = createProducts(1);
        products.get(0).put("ProductName", "Kochjacke PaulinPanzer");
        indexProducts(products, node);
        refreshAllSuggesters();

        suggestions = getSuggestions("ProductName.suggest", "kochjacke paulin", 10);
        assertSuggestions(suggestions, "kochjacke paulinator", "kochjacke pauline", "kochjacke paulinpanzer");

        cleanIndex();
        refreshAllSuggesters();
        suggestions = getSuggestions("ProductName.suggest", "kochjacke paulin", 10);
        assertThat(suggestions.size(), is(0));
    }

    @Test
    public void testThatSuggestionWorksWithSimilarity() throws Exception {
        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "kochjacke bla");
        products.get(1).put("ProductName", "kochjacke blubb");
        products.get(2).put("ProductName", "kochjacke blibb");
        products.get(3).put("ProductName", "kochjacke paul");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochajcke", 10, 0.75f);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, contains("kochjacke"));
    }

    @Test
    public void testThatRefreshingPerIndexWorks() throws Exception {
        createIndexWithMapping("secondproductsindex", node);

        List<Map<String, Object>> products = createProducts(2);
        products.get(0).put("ProductName", "autoreifen");
        products.get(1).put("ProductName", "autorad");
        indexProducts(products, node);
        indexProducts(products, "secondproductsindex", node);

        // get suggestions from both indexes to create fst structures
        getSuggestions("ProductName.suggest", "auto", 10);
        getSuggestions("secondproductsindex", "ProductName.suggest", "auto", 10);

        // index another product
        List<Map<String, Object>> newProducts = createProducts(1);
        newProducts.get(0).put("ProductName", "automatik");
        indexProducts(newProducts, node);
        indexProducts(newProducts, "secondproductsindex", node);

        refreshIndexSuggesters("products");

        List<String> suggestionsFromProductIndex = getSuggestions("ProductName.suggest", "auto", 10);
        List<String> suggestionsFromSecondProductIndex = getSuggestions("secondproductsindex", "ProductName.suggest", "auto", 10);
        assertSuggestions(suggestionsFromProductIndex, "automatik", "autorad", "autoreifen");
        assertSuggestions(suggestionsFromSecondProductIndex, "autorad", "autoreifen");
    }

    @Test
    public void testThatRefreshingPerIndexFieldWorks() throws Exception {
        List<Map<String, Object>> products = createProducts(2);
        products.get(0).put("ProductName", "autoreifen");
        products.get(1).put("ProductName", "autorad");
        indexProducts(products, node);

        getSuggestions("ProductName.suggest", "auto", 10);
        getSuggestions("ProductName.lowercase", "auto", 10);

        List<Map<String, Object>> newProducts = createProducts(1);
        newProducts.get(0).put("ProductName", "automatik");
        indexProducts(newProducts, node);

        refreshFieldSuggesters("products", "ProductName.suggest");

        List<String> suggestionsFromSuggestField = getSuggestions("ProductName.suggest", "auto", 10);
        List<String> suggestionsFromLowercaseField = getSuggestions("ProductName.lowercase", "auto", 10);
        assertSuggestions(suggestionsFromSuggestField, "automatik", "autorad", "autoreifen");
        assertSuggestions(suggestionsFromLowercaseField, "autorad", "autoreifen");
    }

//    @Test
//    public void performanceTest() throws Exception {
//        List<Map<String, Object>> products = createProducts(60000);
//        indexProducts(products);
//
//        System.out.println(measureSuggestTime("a"));
//        System.out.println(measureSuggestTime("aa"));
//        System.out.println(measureSuggestTime("aaa"));
//        System.out.println(measureSuggestTime("aaab"));
//        System.out.println(measureSuggestTime("aaabc"));
//        System.out.println(measureSuggestTime("aaabcd"));
//    }
//
//    private long measureSuggestTime(String search) throws Exception {
//        long start = System.currentTimeMillis();
//        getSuggestions("ProductName.suggest", "aaa", 10);
//        long end = System.currentTimeMillis();
//
//        return end - start;
//    }

    private List<String> getSuggestions(String field, String term, Integer size) throws Exception {
        return getSuggestions(DEFAULT_INDEX, field, term, size);
    }

    private List<String> getSuggestions(String field, String term, Integer size, Float similarity) throws Exception {
        return getSuggestions(DEFAULT_INDEX, field, term, size, similarity);
    }

    private void assertSuggestions(List<String> suggestions, String ... terms) {
        assertThat(suggestions.toString() + "should have size " + terms.length, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private void cleanIndex() {
        node.client().deleteByQuery(new DeleteByQueryRequest("products").types("product").query(QueryBuilders.matchAllQuery())).actionGet();
    }

}

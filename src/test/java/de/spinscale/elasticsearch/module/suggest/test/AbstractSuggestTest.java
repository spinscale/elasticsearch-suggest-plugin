package de.spinscale.elasticsearch.module.suggest.test;

import static de.spinscale.elasticsearch.module.suggest.test.NodeTestHelper.*;
import static de.spinscale.elasticsearch.module.suggest.test.ProductTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSuggestTest {

    protected String clusterName;
    protected Node node;
    protected List<Node> nodes = Lists.newArrayList();
    protected ExecutorService executor;
    public static final String DEFAULT_INDEX = "products";
    public static final String DEFAULT_TYPE  = "product";

    @Parameters
    public static Collection<Object[]> data() {
        // first argument: number of shards, second argument: number of nodes
//        Object[][] data = new Object[][] { { 1,1 } };
        Object[][] data = new Object[][] { { 1, 1 }, { 4, 1 }, { 10, 1 }, { 4, 4 } };
        return Arrays.asList(data);
    }

    public AbstractSuggestTest(int shards, int nodeCount) throws Exception {
        clusterName = "SuggestTest_" + Math.random();
        executor = Executors.newFixedThreadPool(nodeCount);
        List<Future<Node>> nodeFutures = Lists.newArrayList();

        for (int i = 0 ; i < nodeCount ; i++) {
            String nodeName = String.format("node-%02d", i);
            Future<Node> nodeFuture = executor.submit(createNode(clusterName, nodeName, shards));
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
        node.client().admin().indices().prepareDelete("products").execute().actionGet();

        for (Node nodeToStop : nodes) {
            executor.submit(stopNode(nodeToStop));
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES); // maybe there are freaky long gc runs, so wait
    }

    abstract public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception;
    abstract public void refreshAllSuggesters() throws Exception;
    abstract public void refreshIndexSuggesters(String index) throws Exception;
    abstract public void refreshFieldSuggesters(String index, String field) throws Exception;
    abstract public FstStats getStatistics() throws Exception;

    @Test
    public void testThatSimpleSuggestionWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foob", "foobar", "boof");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob", "foobar");
    }

    @Test
    public void testThatAllFieldSuggestionsWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foob", "foobar", "boof");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("_all", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob", "foobar");
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimit() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "fooba", "foobar");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertSuggestions(suggestions, "foo", "fooba");
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimitWithConcreteWord() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "fooba", "foobar");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertSuggestions(suggestions, "foo", "fooba");
    }

    @Test
    public void testThatSuggestionShouldNotContainDuplicates() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foo", "foob");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob");
    }

    @Test
    public void testThatSuggestionShouldWorkOnDifferentFields() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "Kochjacke Pute", "Kochjacke Henne", "Kochjacke Hahn");
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
        List<Map<String, Object>> products = createProducts("ProductName", "Kochjacke Paul", "Kochjacke Pauline",
                "Kochjacke Paulinea");
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
        List<Map<String, Object>> products = createProducts("ProductName", "Kochjacke Paul", "Kochjacke Pauline",
                "Kochjacke Paulinator");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochjacke", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke paul", "kochjacke paulinator", "kochjacke pauline");

        products = createProducts(1);
        products.get(0).put("ProductName", "Kochjacke PaulinPanzer");
        indexProducts(products, node);
        refreshIndex(DEFAULT_INDEX, node);
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
        List<Map<String, Object>> products = createProducts("ProductName", "kochjacke bla", "kochjacke blubb",
                "kochjacke blibb", "kochjacke paul");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochajcke", 10, 0.75f);
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions, contains("kochjacke"));
    }

    @Ignore("This test is useless in this setup, as it may return better/more data than expected and therefore fails")
    @Test
    public void testThatRefreshingPerIndexWorks() throws Exception {
        // having routing ensures that all the data is written into one shard
        // this ensures that when adding the second product it is added to the same shard
        // if it is not added to the same shard, refreshing might not work as expected
        // in case the data is added to a shard where there was no data in before, it is added immediately to the
        // suggestions, this means more results than expected might be returned in the last line of this test
        createIndexWithMapping("secondproductsindex", node);

        List<Map<String, Object>> products = createProducts("ProductName", "autoreifen", "autorad");
        indexProducts(products, DEFAULT_INDEX, "someRoutingKey", node);
        indexProducts(products, "secondproductsindex", "someRoutingKey", node);

        // get suggestions from both indexes to create fst structures
        SuggestionQuery productsQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.suggest", "auto");
        SuggestionQuery secondProductsIndexQuery = new SuggestionQuery("secondproductsindex", DEFAULT_TYPE, "ProductName.suggest", "auto");
        getSuggestions(productsQuery);
        getSuggestions(secondProductsIndexQuery);

        // index another product
        List<Map<String, Object>> newProducts = createProducts("ProductName", "automatik");
        indexProducts(newProducts, DEFAULT_INDEX, "someRoutingKey", node);
        indexProducts(newProducts, "secondproductsindex", "someRoutingKey", node);

        refreshIndexSuggesters("products");

        assertSuggestions(productsQuery, "automatik", "autorad", "autoreifen");
        assertSuggestions(secondProductsIndexQuery, "autorad", "autoreifen");
    }

    @Ignore("This test is useless in this setup, as it may return better/more data than expected and therefore fails")
    @Test
    public void testThatRefreshingPerIndexFieldWorks() throws Exception {
        // having routing ensures that all the data is written into one shard
        // this ensures that when adding the second product it is added to the same shard
        // if it is not added to the same shard, refreshing might not work as expected
        // in case the data is added to a shard where there was no data in before, it is added immediately to the
        // suggestions, this means more results than expected might be returned in the last line of this test
        List<Map<String, Object>> products = createProducts("ProductName", "autoreifen", "autorad");
        indexProducts(products, DEFAULT_INDEX, "someRoutingKey", node);

        SuggestionQuery suggestionQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.suggest", "auto");
        SuggestionQuery lowerCaseQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.lowercase", "auto");
        getSuggestions(suggestionQuery);
        getSuggestions(lowerCaseQuery);

        List<Map<String, Object>> newProducts = createProducts("ProductName", "automatik");
        indexProducts(newProducts, DEFAULT_INDEX, "someRoutingKey", node);

        refreshFieldSuggesters("products", "ProductName.suggest");

        assertSuggestions(suggestionQuery, "automatik", "autorad", "autoreifen");
        assertSuggestions(lowerCaseQuery, "autorad", "autoreifen");
    }

    @Test
    public void testThatAnalyzingSuggesterWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, node);

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").analyzer("standard").size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3");
    }

    @Test
    public void testThatAnalyzingSuggesterSupportsStopWords() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, node);

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").indexAnalyzer("suggest_analyzer_stopwords").queryAnalyzer("suggest_analyzer_stopwords").size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3", "the BMW 320");
    }

    @Test
    public void testThatFuzzySuggesterWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, node);

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "bwm")
                .suggestType("fuzzy").analyzer("standard").size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3");
    }

    @Test
    public void testThatFlushForcesReloadingOfAllFieldsWithoutErrors() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318");
        indexProducts(products, node);

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "bwm").suggestType("full");
        getSuggestions(query);

        // add data to index and flush
        // indexProducts(createProducts("ProductName", "BMW 320"), node);
        node.client().prepareIndex(DEFAULT_INDEX, DEFAULT_TYPE, "foo").setSource(createProducts(1).get(0)).execute().actionGet();
        node.client().admin().indices().prepareFlush(DEFAULT_INDEX).execute().actionGet();
        getSuggestions(query);
    }

    @Test
    public void gettingStatisticsShouldWork() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName",
                "BMW 318", "BMW 528", "BMW M3", "the BMW 320", "VW Jetta");
        indexProducts(products, node);

        FstStats emptyFstStats = getStatistics();
        assertThat(emptyFstStats.getStats().keySet(), hasSize(0));
        assertThat(getFstSizeSum(emptyFstStats), equalTo(0L));

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").analyzer("suggest_analyzer_stopwords").size(10);
        getSuggestions(query);

        FstStats filledFstStats = getStatistics();
        assertThat(filledFstStats.getStats().keySet(), hasSize(greaterThanOrEqualTo(1)));

        List<List<FstStats.FstIndexShardStats>> allStats = Lists.newArrayList(filledFstStats.getStats().values());
        assertThat(allStats.get(0).get(0).fieldName(), is("analyzingsuggester-ProductName.keyword"));
        assertThat(allStats.get(0).get(0).shardId(), greaterThanOrEqualTo(0));
        assertThat(getFstSizeSum(filledFstStats), greaterThan(0L));
    }

    private long getFstSizeSum(FstStats fstStats) {
        long totalFstSize = 0;

        for (List<FstStats.FstIndexShardStats> stats : fstStats.getStats().values()) {
            for (FstStats.FstIndexShardStats indexShardStats : stats) {
                totalFstSize += indexShardStats.size();
            }
        }

        return totalFstSize;
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
        return getSuggestions(new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, field, term).size(size));
    }

    private List<String> getSuggestions(String field, String term, Integer size, Float similarity) throws Exception {
        return getSuggestions(new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, field, term).size(size).similarity(similarity));
    }

    private void assertSuggestions(List<String> suggestions, String ... terms) {
        assertThat(suggestions.toString() + " should have size " + terms.length, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private void assertSuggestions(SuggestionQuery query, String ... terms) throws Exception {
        List<String> suggestions = getSuggestions(query);
        String assertionError = String.format("%s for query %s should be %s", suggestions, query, Arrays.asList(terms));
        assertThat(assertionError, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private void cleanIndex() {
        node.client().deleteByQuery(new DeleteByQueryRequest("products").types("product").query(QueryBuilders.matchAllQuery())).actionGet();
    }
}

package de.spinscale.elasticsearch.module.suggest.test;

import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequest;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static de.spinscale.elasticsearch.module.suggest.test.NodeTestHelper.createIndexWithMapping;
import static de.spinscale.elasticsearch.module.suggest.test.ProductTestHelper.createProducts;
import static de.spinscale.elasticsearch.module.suggest.test.ProductTestHelper.indexProducts;
import static org.hamcrest.Matchers.*;

@TestLogging("_root:info")
@Ignore
public abstract class AbstractSuggestTest extends ElasticsearchIntegrationTest {

    public static final String DEFAULT_INDEX = "products";
    public static final String DEFAULT_TYPE  = "product";

    @Before
    public void createIndex() throws Exception {
        String settingsData = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/product.json"));
        CreateIndexResponse createIndexResponse = client().admin().indices().prepareCreate(DEFAULT_INDEX)
                .setSource(settingsData).execute().actionGet();
        assertThat(createIndexResponse.isAcknowledged(), is(true));

        client().admin().cluster().prepareHealth(DEFAULT_INDEX).setWaitForGreenStatus().execute().actionGet();
    }

    abstract public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception;
    abstract public void refreshAllSuggesters() throws Exception;
    abstract public void refreshIndexSuggesters(String index) throws Exception;
    abstract public void refreshFieldSuggesters(String index, String field) throws Exception;
    abstract public FstStats getStatistics() throws Exception;

    @Test
    public void testThatSimpleSuggestionWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foob", "foobar", "boof");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob", "foobar");
    }

    @Test
    public void testThatAllFieldSuggestionsWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foob", "foobar", "boof");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("_all", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob", "foobar");
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimit() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "fooba", "foobar");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertSuggestions(suggestions, "foo", "fooba");
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimitWithConcreteWord() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "fooba", "foobar");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2);
        assertSuggestions(suggestions, "foo", "fooba");
    }

    @Test
    public void testThatSuggestionShouldNotContainDuplicates() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "foo", "foo", "foob");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertSuggestions(suggestions, "foo", "foob");
    }

    @Test
    public void testThatSuggestionShouldWorkOnDifferentFields() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "Kochjacke Pute", "Kochjacke Henne", "Kochjacke Hahn");
        products.get(0).put("Description", "Kochjacke Pute");
        products.get(1).put("Description", "Kochjacke Henne");
        products.get(2).put("Description", "Kochjacke Hahn");
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochjacke", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke hahn", "kochjacke henne", "kochjacke pute");

        suggestions = getSuggestions("Description", "Kochjacke", 10);
        assertSuggestions(suggestions, "Kochjacke Hahn", "Kochjacke Henne", "Kochjacke Pute");
    }

    @Test
    public void testThatSuggestionShouldWorkWithWhitespaces() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "Kochjacke Paul", "Kochjacke Pauline",
                "Kochjacke Paulinea");
        indexProducts(products, client());

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
        indexProducts(products, client());

        List<String> suggestions = getSuggestions("ProductName.suggest", "kochjacke", 10);
        assertSuggestions(suggestions, "kochjacke", "kochjacke paul", "kochjacke paulinator", "kochjacke pauline");

        products = createProducts(1);
        products.get(0).put("ProductName", "Kochjacke PaulinPanzer");
        indexProducts(products, client());
        refresh();
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
        indexProducts(products, client());

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
        createIndexWithMapping("secondproductsindex", client());

        List<Map<String, Object>> products = createProducts("ProductName", "autoreifen", "autorad");
        indexProducts(products, DEFAULT_INDEX, "someRoutingKey", client());
        indexProducts(products, "secondproductsindex", "someRoutingKey", client());

        // get suggestions from both indexes to create fst structures
        SuggestionQuery productsQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.suggest", "auto");
        SuggestionQuery secondProductsIndexQuery = new SuggestionQuery("secondproductsindex", DEFAULT_TYPE, "ProductName.suggest", "auto");
        getSuggestions(productsQuery);
        getSuggestions(secondProductsIndexQuery);

        // index another product
        List<Map<String, Object>> newProducts = createProducts("ProductName", "automatik");
        indexProducts(newProducts, DEFAULT_INDEX, "someRoutingKey", client());
        indexProducts(newProducts, "secondproductsindex", "someRoutingKey", client());

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
        indexProducts(products, DEFAULT_INDEX, "someRoutingKey", client());

        SuggestionQuery suggestionQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.suggest", "auto");
        SuggestionQuery lowerCaseQuery = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.lowercase", "auto");
        getSuggestions(suggestionQuery);
        getSuggestions(lowerCaseQuery);

        List<Map<String, Object>> newProducts = createProducts("ProductName", "automatik");
        indexProducts(newProducts, DEFAULT_INDEX, "someRoutingKey", client());

        refreshFieldSuggesters("products", "ProductName.suggest");

        assertSuggestions(suggestionQuery, "automatik", "autorad", "autoreifen");
        assertSuggestions(lowerCaseQuery, "autorad", "autoreifen");
    }

    @Test
    public void testThatAnalyzingSuggesterWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, client());

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").analyzer("simple").size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3");
    }

    @Test
    public void testThatAnalyzingSuggesterSupportsStopWords() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, client());

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").indexAnalyzer("stop").queryAnalyzer("stop")
                .preservePositionIncrements(false).size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3", "the BMW 320");
    }

    @Test
    public void testThatFuzzySuggesterWorks() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318", "BMW 528", "BMW M3",
                "the BMW 320", "VW Jetta");
        indexProducts(products, client());

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "bwm")
                .suggestType("fuzzy").analyzer("standard").size(10);
        List<String> suggestions = getSuggestions(query);

        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3");
    }

    @Test
    public void testThatFlushForcesReloadingOfAllFieldsWithoutErrors() throws Exception {
        List<Map<String, Object>> products = createProducts("ProductName", "BMW 318");
        indexProducts(products, client());

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "bwm").suggestType("full");
        getSuggestions(query);

        // add data to index and flush
        // indexProducts(createProducts("ProductName", "BMW 320"), node);
        client().prepareIndex(DEFAULT_INDEX, DEFAULT_TYPE, "foo").setSource(createProducts(1).get(0)).execute().actionGet();
        client().admin().indices().prepareFlush(DEFAULT_INDEX).execute().actionGet();
        getSuggestions(query);
    }

    @Test
    public void gettingStatisticsShouldWork() throws Exception {
        // needed to make sure that we hit the already queried shards for stats, the other are empty
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("index.number_of_replicas", 0)
                .build();
        UpdateSettingsResponse response = client().admin().indices().prepareUpdateSettings(DEFAULT_INDEX).setSettings(settings).get();
        assertThat(response.isAcknowledged(), is(true));

        List<Map<String, Object>> products = createProducts("ProductName",
                "BMW 318", "BMW 528", "BMW M3", "the BMW 320", "VW Jetta");
        indexProducts(products, client());

        FstStats emptyFstStats = getStatistics();
        assertThat(emptyFstStats.getStats(), hasSize(0));
        assertThat(getFstSizeSum(emptyFstStats), equalTo(0L));

        SuggestionQuery query = new SuggestionQuery(DEFAULT_INDEX, DEFAULT_TYPE, "ProductName.keyword", "b")
                .suggestType("full").analyzer("stop").size(10);
        List<String> suggestions = getSuggestions(query);
        assertSuggestions(suggestions, "BMW 318", "BMW 528", "BMW M3", "the BMW 320");

        FstStats filledFstStats = getStatistics();
        assertThat(filledFstStats.getStats(), hasSize(greaterThanOrEqualTo(1)));

        List<FstStats.FstIndexShardStats> allStats = Lists.newArrayList(filledFstStats.getStats());
        assertThat(allStats.get(0).getShardId().id(), greaterThanOrEqualTo(0));
        assertThat(getFstSizeSum(filledFstStats), greaterThan(0L));
    }

    private long getFstSizeSum(FstStats fstStats) {
        long totalFstSize = 0;

        for (FstStats.FstIndexShardStats stats : fstStats.getStats()) {
            totalFstSize += stats.getSizeInBytes();
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
        String msg = String.format("%s should have size %s, content %s", suggestions, terms.length, Arrays.asList(terms));
        assertThat(msg, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private void assertSuggestions(SuggestionQuery query, String ... terms) throws Exception {
        List<String> suggestions = getSuggestions(query);
        String assertionError = String.format("%s for query %s should be %s", suggestions, query, Arrays.asList(terms));
        assertThat(assertionError, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private void cleanIndex() {
        client().deleteByQuery(new DeleteByQueryRequest("products").types("product").query(QueryBuilders.matchAllQuery())).actionGet();
    }
}

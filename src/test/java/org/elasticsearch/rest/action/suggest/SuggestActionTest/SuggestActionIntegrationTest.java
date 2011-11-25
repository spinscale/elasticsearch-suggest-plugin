package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import static org.elasticsearch.rest.action.suggest.SuggestActionTest.SuggestTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@RunWith(value = Parameterized.class)
public class SuggestActionIntegrationTest {

    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private Node node;
    private int numberOfShards;

    public SuggestActionIntegrationTest(int shards) {
        numberOfShards = shards;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 } };
        return Arrays.asList(data);
    }

    @Before
    public void startElasticSearch() throws Exception {
        String randStr = "IntegrationTestCluster" + Math.random();
        Builder settingsBuilder = ImmutableSettings.settingsBuilder();
        String config = IOUtils.toString(getClass().getResourceAsStream("/elasticsearch.yml"));
        settingsBuilder = settingsBuilder.loadFromSource(config);

        settingsBuilder.put("gateway.type", "none");
        settingsBuilder.put("cluster.name", randStr);
        System.out.println("Number of shards: " + numberOfShards);
        settingsBuilder.put("index.number_of_shards", numberOfShards);

        LogConfigurator.configure(settingsBuilder.build());

        node = NodeBuilder.nodeBuilder().settings(settingsBuilder.build()).node();
        String mapping = IOUtils.toString(getClass().getResourceAsStream("/product.json"));
        node.client().admin().indices().prepareCreate("products").execute().actionGet();
        node.client().admin().indices().preparePutMapping("products").setType("product").setSource(mapping).execute().actionGet();
    }

    @After
    public void stopServers() throws Exception {
        node.client().close();
        node.close();
    }

    @Test
    public void testThatSimpleSuggestionWorks() throws Exception {
        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foobar");
        products.get(3).put("ProductName", "boof");
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 10);
        assertThat(suggestions, hasSize(3));
        assertThat(suggestions, contains("foo", "foob", "foobar"));
    }

    @Test
    public void testThatSimpleSuggestionShouldSupportLimit() throws Exception {
        List<Map<String, Object>> products = createProducts(3);
        products.get(0).put("ProductName", "foob");
        products.get(1).put("ProductName", "fooba");
        products.get(2).put("ProductName", "foobar");
        assertThat(products.size(), is(3));
        indexProducts(products, node);

        List<String> suggestions = getSuggestions("ProductName.suggest", "foo", 2, 3);
        assertThat(suggestions + " is not correct", suggestions, hasSize(2));
        assertThat(suggestions, contains("foob", "fooba"));
    }

    @Ignore("Did not yet investigate why this test does not work, the only difference is the productname of the first product, which matches the searchterm")
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
        assertThat(suggestions, contains("foob", "fooba"));
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

        suggestions = getSuggestions("ProductName.suggest", "kochjacke paulin", 10);
        assertSuggestions(suggestions, "kochjacke paulinator", "kochjacke pauline", "kochjacke paulinpanzer");
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

    private void assertSuggestions(List<String> suggestions, String ... terms) {
        assertThat(suggestions.toString() + "should have size " + terms.length, suggestions, hasSize(terms.length));
        assertThat("Suggestions are: " + suggestions, suggestions, contains(terms));
    }

    private List<String> getSuggestions(String field, String term, Integer size) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        return getSuggestions(field, term, size, size);
   }

    @SuppressWarnings("unchecked")
    private List<String> getSuggestions(String field, String term, Integer size, Integer totalCount) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        String json = String.format("{ \"field\": \"%s\", \"term\": \"%s\", \"size\": \"%s\" }", field, term, size);
        Response r = httpClient.preparePost("http://localhost:9200/products/product/_suggest").setBody(json).execute().get();
        assertThat(r.getStatusCode(), is(200));
        XContentParser parser = JsonXContent.jsonXContent.createParser(r.getResponseBody());
        Map<String, Object> jsonResponse = parser.map();

        // Check for paging
        if (totalCount > size) {
            assertThat(jsonResponse, hasKey("count"));
            assertThat((Integer)jsonResponse.get("count"), is(totalCount));
        }
        assertThat(jsonResponse, hasKey("suggestions"));
        return (List<String>) jsonResponse.get("suggestions");
    }

}

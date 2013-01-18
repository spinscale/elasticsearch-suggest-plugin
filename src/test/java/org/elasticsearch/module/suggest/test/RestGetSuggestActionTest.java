package org.elasticsearch.module.suggest.test;

import static org.elasticsearch.module.suggest.test.NodeTestHelper.*;
import static org.elasticsearch.module.suggest.test.ProductTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class RestGetSuggestActionTest {

    private final AsyncHttpClient httpClient = new AsyncHttpClient();
    private Node node;

    @Before
    public void startNode() throws Exception {
        node = createNode("foo", 1).call().start();

        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foobar");
        createIndexWithMapping("products", node);
        indexProducts(products, node);
        refreshAllSuggesters();
    }

    @After
    public void closeHttpClient() {
        httpClient.close();
        node.stop();
    }

    @Ignore("AsyncHttpClient does not allow body in GET requests - need to change")
    @Test
    public void testThatSuggestionsShouldWorkWithGetRequestBody() throws Exception {
        String response = httpClient.prepareGet("http://localhost:9200/products/product/_suggest").
                setBody(createJSONQuery("ProductName.suggest", "foo")).
                execute().get().getResponseBody();
        List<String> suggestions = getSuggestionsFromResponse(response);
        assertThat(suggestions, containsInAnyOrder("foo", "foob", "foobar"));
    }

    @Test
    public void testThatSuggestionsShouldWorkWithCallbackAndGetRequestParameter() throws Exception {
        String query = URLEncoder.encode(createJSONQuery("ProductName.suggest", "foobar"), "UTF8");
        String queryString = "callback=mycallback&source=" + query;
        String response = httpClient.prepareGet("http://localhost:9200/products/product/_suggest?" + queryString).
                execute().get().getResponseBody();
        assertThat(response, is("mycallback({\"suggestions\":[\"foobar\"],\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0}});"));
    }

    private void refreshAllSuggesters() throws Exception {
        Response r = httpClient.preparePost("http://localhost:9200/_suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    private String createJSONQuery(String field, String term) {
        return String.format("{ \"field\": \"%s\", \"term\": \"%s\" }", field, term);
    }

    @SuppressWarnings("unchecked")
    private List<String> getSuggestionsFromResponse(String response) throws IOException {
        XContentParser parser = JsonXContent.jsonXContent.createParser(response);
        Map<String, Object> jsonResponse = parser.map();
        assertThat(jsonResponse, hasKey("suggestions"));
        return (List<String>) jsonResponse.get("suggestions");
    }

}

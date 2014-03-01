package de.spinscale.elasticsearch.module.suggest.test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import static de.spinscale.elasticsearch.module.suggest.test.NodeTestHelper.createIndexWithMapping;
import static de.spinscale.elasticsearch.module.suggest.test.ProductTestHelper.createProducts;
import static de.spinscale.elasticsearch.module.suggest.test.ProductTestHelper.indexProducts;
import static org.hamcrest.Matchers.*;

@ElasticsearchIntegrationTest.ClusterScope(scope=ElasticsearchIntegrationTest.Scope.SUITE)
public class RestGetSuggestActionTest extends ElasticsearchIntegrationTest {

    private final AsyncHttpClient httpClient = new AsyncHttpClient();
    private int port;

    @Before
    public void startNode() throws Exception {
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().setHttp(true).execute().actionGet();
        port = ((InetSocketTransportAddress) response.getNodes()[0].getHttp().address().boundAddress()).address().getPort();

        List<Map<String, Object>> products = createProducts(4);
        products.get(0).put("ProductName", "foo");
        products.get(1).put("ProductName", "foob");
        products.get(2).put("ProductName", "foobar");
        createIndexWithMapping("products", client());
        indexProducts(products, client());
        refreshAllSuggesters();
    }

    @After
    public void closeResources() {
        httpClient.close();
    }

    @Ignore("AsyncHttpClient does not allow body in GET requests - need to change")
    @Test
    public void testThatSuggestionsShouldWorkWithGetRequestBody() throws Exception {
        String response = httpClient.prepareGet("http://localhost:" + port + "/products/product/__suggest").
                setBody(createJSONQuery("ProductName.suggest", "foo")).
                execute().get().getResponseBody();
        List<String> suggestions = getSuggestionsFromResponse(response);
        assertThat(suggestions, containsInAnyOrder("foo", "foob", "foobar"));
    }

    @Test
    public void testThatSuggestionsShouldWorkWithCallbackAndGetRequestParameter() throws Exception {
        String query = URLEncoder.encode(createJSONQuery("ProductName.suggest", "foobar"), "UTF8");
        String queryString = "callback=mycallback&source=" + query;
        String response = httpClient.prepareGet("http://localhost:" + port + "/products/product/__suggest?" + queryString).
                execute().get().getResponseBody();
        assertThat(response, startsWith("mycallback({\"_shards\":{\"total\""));
        assertThat(response, endsWith("\"suggestions\":[\"foobar\"]});"));
    }

    private void refreshAllSuggesters() throws Exception {
        Response r = httpClient.preparePost("http://localhost:" + port + "/__suggestRefresh").execute().get();
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

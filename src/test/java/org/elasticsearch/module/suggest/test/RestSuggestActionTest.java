package org.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@RunWith(value = Parameterized.class)
public class RestSuggestActionTest extends AbstractSuggestTest {

    private final AsyncHttpClient httpClient = new AsyncHttpClient();

    public RestSuggestActionTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    @After
    public void closeHttpClient() {
        httpClient.close();
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size, Float similarity) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        String json = createJSONQuery(field, term, size, similarity);
        String url = "http://localhost:9200/" + index + "/product/_suggest";
        Response r = httpClient.preparePost(url).setBody(json).execute().get();
        assertThat(r.getStatusCode(), is(200));
//        System.out.println("REQ : " + json);
//        System.out.println("RESP: " + r.getResponseBody());

        return getSuggestionsFromResponse(r.getResponseBody());
   }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        return getSuggestions(index, field, term, size, null);
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        Response r = httpClient.preparePost("http://localhost:9200/_suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        Response r = httpClient.preparePost("http://localhost:9200/"+ index + "/product/_suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        String jsonBody = String.format("{ \"field\": \"%s\" } ", field);

        Response r = httpClient.preparePost("http://localhost:9200/" + index + "/product/_suggestRefresh").setBody(jsonBody).execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    private String createJSONQuery(String field, String term, Integer size, Float similarity) {
        StringBuilder query = new StringBuilder("{");
        query.append(String.format("\"field\": \"%s\", ", field));
        query.append(String.format("\"term\": \"%s\"", term));
        if (size != null) {
            query.append(String.format(", \"size\": \"%s\"", size));
        }
        if (similarity != null && similarity > 0.0 && similarity < 1.0) {
            query.append(String.format(", \"similarity\": \"%s\"", similarity));
        }
        query.append("}");

        return query.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> getSuggestionsFromResponse(String response) throws IOException {
        XContentParser parser = JsonXContent.jsonXContent.createParser(response);
        Map<String, Object> jsonResponse = parser.map();
        assertThat(jsonResponse, hasKey("suggestions"));
        return (List<String>) jsonResponse.get("suggestions");
    }

}

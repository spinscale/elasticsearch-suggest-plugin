package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

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
public class SuggestActionIntegrationTest extends AbstractSuggestTest {

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    public SuggestActionIntegrationTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    @After
    public void closeHttpClient() {
        httpClient.close();
    }

    @Override
    public List<String> getSuggestions(String field, String term, Integer size, Float similarity) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        String json = createJSONQuery(field, term, size, similarity);
        Response r = httpClient.preparePost("http://localhost:9200/products/product/_suggest").setBody(json).execute().get();
        assertThat(r.getStatusCode(), is(200));

        return getSuggestionsFromResponse(r.getResponseBody());
   }

    @Override
    public List<String> getSuggestions(String field, String term, Integer size) throws IllegalArgumentException, InterruptedException, ExecutionException, IOException {
        return getSuggestions(field, term, size, null);
    }

    @Override
    public void refreshSuggestIndex() throws Exception {
        // TODO: Issue rest call, sleeping sucks
        Thread.sleep(2000);
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

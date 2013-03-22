package de.spinscale.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception {
        String json = createJSONQuery(suggestionQuery);

        String url = "http://localhost:9200/" + suggestionQuery.index + "/" + suggestionQuery.type + "/__suggest";
        Response r = httpClient.preparePost(url).setBody(json).execute().get();
        assertThat(r.getStatusCode(), is(200));
//        System.out.println("REQ : " + json);
//        System.out.println("RESP: " + r.getResponseBody());

        return getSuggestionsFromResponse(r.getResponseBody());
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        Response r = httpClient.preparePost("http://localhost:9200/__suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        Response r = httpClient.preparePost("http://localhost:9200/"+ index + "/product/__suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        String jsonBody = String.format("{ \"field\": \"%s\" } ", field);

        Response r = httpClient.preparePost("http://localhost:9200/" + index + "/product/__suggestRefresh").setBody(jsonBody).execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    private String createJSONQuery(SuggestionQuery suggestionQuery) {
        StringBuilder query = new StringBuilder("{");
        query.append(String.format("\"field\": \"%s\", ", suggestionQuery.field));
        query.append(String.format("\"term\": \"%s\"", suggestionQuery.term));
        if (suggestionQuery.size != null) {
            query.append(String.format(", \"size\": \"%s\"", suggestionQuery.size));
        }
        if (suggestionQuery.suggestType != null) {
            query.append(String.format(", \"type\": \"%s\"", suggestionQuery.suggestType));
        }
        if (suggestionQuery.similarity != null && suggestionQuery.similarity > 0.0 && suggestionQuery.similarity < 1.0) {
            query.append(String.format(", \"similarity\": \"%s\"", suggestionQuery.similarity));
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

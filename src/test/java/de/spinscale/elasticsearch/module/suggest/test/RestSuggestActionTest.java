package de.spinscale.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RunWith(value = Parameterized.class)
public class RestSuggestActionTest extends AbstractSuggestTest {

    private final AsyncHttpClient httpClient = new AsyncHttpClient();
    private final int port;

    public RestSuggestActionTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
        NodesInfoResponse response = node.client().admin().cluster().prepareNodesInfo().setHttp(true).execute().actionGet();
        port = ((InetSocketTransportAddress) response.getNodes()[0].getHttp().address().boundAddress()).address().getPort();
    }

    @After
    public void closeHttpClient() {
        httpClient.close();
    }

    @Override
    public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception {
        String json = createJSONQuery(suggestionQuery);

        String url = "http://localhost:" + port + "/" + suggestionQuery.index + "/" + suggestionQuery.type + "/__suggest";
        Response r = httpClient.preparePost(url).setBody(json).execute().get();
        assertThat(r.getStatusCode(), is(200));
        assertThatResponseHasNoShardFailures(r);

//        System.out.println("REQ : " + json);
//        System.out.println("RESP: " + r.getResponseBody());

        return getSuggestionsFromResponse(r.getResponseBody());
    }

    private void assertThatResponseHasNoShardFailures(Response r) throws IOException {
        XContentParser parser = JsonXContent.jsonXContent.createParser(r.getResponseBody());
        Map<String, Object> jsonResponse = parser.mapAndClose();
        assertThat(jsonResponse, hasKey("_shards"));
        Map<String, Object> shardResponse = (Map<String, Object>) jsonResponse.get("_shards");
        assertThat(shardResponse, not(hasKey("failures")));
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        Response r = httpClient.preparePost("http://localhost:" + port + "/__suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        Response r = httpClient.preparePost("http://localhost:" + port + "/"+ index + "/product/__suggestRefresh").execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        String jsonBody = String.format("{ \"field\": \"%s\" } ", field);

        Response r = httpClient.preparePost("http://localhost:"+ port +"/" + index + "/product/__suggestRefresh").setBody(jsonBody).execute().get();
        assertThat(r.getStatusCode(), is(200));
    }

    @Override
    public FstStats getStatistics() throws Exception {
        Response r = httpClient.prepareGet("http://localhost:" + port + "/__suggestStatistics").execute().get();
        assertThat(r.getStatusCode(), is(200));
        System.out.println(r.getResponseBody());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootObj = objectMapper.readTree(r.getResponseBody());
        FstStats fstStats = new FstStats();
        ObjectNode jsonFstStats = (ObjectNode) rootObj.get("fstStats");
        Iterator<Map.Entry<String, JsonNode>> nodes = jsonFstStats.getFields();

        while (nodes.hasNext()) {
            Map.Entry<String, JsonNode> fstStatsNodeEntry =  nodes.next();

            if (fstStatsNodeEntry.getValue().isArray()) {
                ArrayNode jsonNode = (ArrayNode) fstStatsNodeEntry.getValue();
                Iterator<JsonNode> shardFieldDataIterator = jsonNode.iterator();

                List<FstStats.FstIndexShardStats> stats = Lists.newArrayList();
                while (shardFieldDataIterator.hasNext()) {
                    JsonNode shardFieldData =  shardFieldDataIterator.next();
                    if (shardFieldData.isObject()) {
                        ObjectNode node = (ObjectNode) shardFieldData;
                        Map.Entry<String, JsonNode> entry = node.getFields().next();
                        String fieldName = entry.getKey();
                        long fieldValue = entry.getValue().getLongValue();
                        FstStats.FstIndexShardStats fstIndexShardStats = new FstStats.FstIndexShardStats(0, fieldName, fieldValue);
                        stats.add(fstIndexShardStats);
                    }
                }
                fstStats.getStats().put(fstStatsNodeEntry.getKey(), stats);
            }

        }

        return fstStats;
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
        if (Strings.hasLength(suggestionQuery.indexAnalyzer)) {
            query.append(String.format(", \"indexAnalyzer\": \"%s\"", suggestionQuery.indexAnalyzer));
        }
        if (Strings.hasLength(suggestionQuery.queryAnalyzer)) {
            query.append(String.format(", \"queryAnalyzer\": \"%s\"", suggestionQuery.queryAnalyzer));
        }
        if (Strings.hasLength(suggestionQuery.analyzer)) {
            query.append(String.format(", \"analyzer\": \"%s\"", suggestionQuery.analyzer));
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

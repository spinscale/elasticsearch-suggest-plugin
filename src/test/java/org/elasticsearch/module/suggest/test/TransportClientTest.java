package org.elasticsearch.module.suggest.test;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;
import org.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class TransportClientTest extends AbstractSuggestTest {

    public TransportClientTest(int shards, int nodeCount) throws Exception {
        super(shards, nodeCount);
    }

    private TransportClient client;

    @Before
    public void startElasticSearch() throws IOException {
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).put("node.client", true).build();
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
    }

    @After
    public void closeClient() throws Exception {
        client.close();
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size, Float similarity) throws Exception {
        SuggestRequestBuilder builder = new SuggestRequestBuilder(client).setIndices(index).field(field).term(term).size(size).similarity(similarity);
        return builder.execute().actionGet().suggestions();
    }

    @Override
    public List<String> getSuggestions(String index, String field, String term, Integer size) throws Exception {
        SuggestRequestBuilder builder = new SuggestRequestBuilder(client).setIndices(index).field(field).term(term).size(size);
        return builder.execute().actionGet().suggestions();
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client);
        builder.execute().actionGet();
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client).setIndices(index);
        builder.execute().actionGet();
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client).setIndices(index).setField(field);
        builder.execute().actionGet();
    }
}

package de.spinscale.elasticsearch.module.suggest.test;

import java.io.IOException;
import java.util.List;

import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestStatisticsRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
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
    public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception {
        SuggestRequestBuilder builder = new SuggestRequestBuilder(client)
                .setIndices(suggestionQuery.index)
                .field(suggestionQuery.field)
                .term(suggestionQuery.term);

        if (suggestionQuery.size != null) {
            builder.size(suggestionQuery.size);
        }
        if (suggestionQuery.similarity != null && suggestionQuery.similarity > 0.0 && suggestionQuery.similarity < 1.0) {
            builder.similarity(suggestionQuery.similarity);
        }
        if (suggestionQuery.suggestType != null) {
            builder.suggestType(suggestionQuery.suggestType);
        }
        if (Strings.hasLength(suggestionQuery.queryAnalyzer)) {
            builder.queryAnalyzer(suggestionQuery.queryAnalyzer);
        }
        if (Strings.hasLength(suggestionQuery.indexAnalyzer)) {
            builder.indexAnalyzer(suggestionQuery.indexAnalyzer);
        }
        if (Strings.hasLength(suggestionQuery.analyzer)) {
            builder.analyzer(suggestionQuery.analyzer);
        }

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

    @Override
    public FstStats getStatistics() throws Exception {
        SuggestStatisticsRequestBuilder builder = new SuggestStatisticsRequestBuilder(client);
        return builder.execute().actionGet().fstStats();
    }
}

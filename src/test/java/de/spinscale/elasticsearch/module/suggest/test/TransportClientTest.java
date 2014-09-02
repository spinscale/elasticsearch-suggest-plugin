package de.spinscale.elasticsearch.module.suggest.test;

import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import de.spinscale.elasticsearch.action.suggest.suggest.SuggestResponse;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestStatisticsRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.Transport;
import org.junit.After;

import java.util.List;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

@ClusterScope(scope=Scope.SUITE, transportClientRatio = 0.0)
public class TransportClientTest extends AbstractSuggestTest {

    // TODO: Remove this class, once we can set transport client settings programmatically and then remove the transportclient ratio from all tests

    private TransportClient transportClient;

    private TransportClient getTransportClient() {
        if (transportClient == null) {
            transportClient = new TransportClient(settingsBuilder()
                    .put("cluster.name", internalCluster().getClusterName())
                    .put("name", "programmatic_transport_client")
                    .put("client.transport.nodes_sampler_interval", "1s")
                    .build());
            Transport transport = internalCluster().getDataNodeInstance(Transport.class);
            transportClient.addTransportAddress(transport.boundAddress().publishAddress());
        }

        return transportClient;
    }

    @After
    public void closeTransportClient() {
        if (transportClient != null) {
            transportClient.close();
        }
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return settingsBuilder().put(super.nodeSettings(nodeOrdinal))
                .put("node.mode", "network")
                .build();
    }

    @Override
    public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception {
        SuggestRequestBuilder builder = new SuggestRequestBuilder(getTransportClient())
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
        builder.preservePositionIncrements(suggestionQuery.preservePositionIncrements);

        SuggestResponse suggestResponse = builder.execute().actionGet();
        assertThat(suggestResponse.getShardFailures(), is(emptyArray()));

        return suggestResponse.suggestions();
    }

    @Override
    public void refreshAllSuggesters() throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(getTransportClient());
        builder.execute().actionGet();
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(getTransportClient()).setIndices(index);
        builder.execute().actionGet();
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(getTransportClient()).setIndices(index).setField(field);
        builder.execute().actionGet();
    }

    @Override
    public FstStats getStatistics() throws Exception {
        SuggestStatisticsRequestBuilder builder = new SuggestStatisticsRequestBuilder(getTransportClient());
        return builder.execute().actionGet().fstStats();
    }
}

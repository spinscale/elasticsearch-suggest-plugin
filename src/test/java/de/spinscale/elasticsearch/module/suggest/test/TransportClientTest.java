package de.spinscale.elasticsearch.module.suggest.test;

import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import de.spinscale.elasticsearch.action.suggest.suggest.SuggestResponse;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRefreshRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import de.spinscale.elasticsearch.client.action.suggest.SuggestStatisticsRequestBuilder;
import org.elasticsearch.common.Strings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;

import java.util.List;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

@ElasticsearchIntegrationTest.ClusterScope(scope=ElasticsearchIntegrationTest.Scope.SUITE, transportClientRatio = 1.0)
public class TransportClientTest extends AbstractSuggestTest {

    @Override
    public List<String> getSuggestions(SuggestionQuery suggestionQuery) throws Exception {
        SuggestRequestBuilder builder = new SuggestRequestBuilder(client())
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
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client());
        builder.execute().actionGet();
    }

    @Override
    public void refreshIndexSuggesters(String index) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client()).setIndices(index);
        builder.execute().actionGet();
    }

    @Override
    public void refreshFieldSuggesters(String index, String field) throws Exception {
        SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client()).setIndices(index).setField(field);
        builder.execute().actionGet();
    }

    @Override
    public FstStats getStatistics() throws Exception {
        SuggestStatisticsRequestBuilder builder = new SuggestStatisticsRequestBuilder(client());
        return builder.execute().actionGet().fstStats();
    }
}

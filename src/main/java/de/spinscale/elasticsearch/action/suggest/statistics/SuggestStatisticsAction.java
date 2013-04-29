package de.spinscale.elasticsearch.action.suggest.statistics;

import de.spinscale.elasticsearch.client.action.suggest.SuggestStatisticsRequestBuilder;
import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

public class SuggestStatisticsAction extends Action<SuggestStatisticsRequest, SuggestStatisticsResponse, SuggestStatisticsRequestBuilder> {

    public static final SuggestStatisticsAction INSTANCE = new SuggestStatisticsAction();
    public static final String NAME = "suggestStatistics";

    private SuggestStatisticsAction() {
        super(NAME);
    }

    @Override
    public SuggestStatisticsRequestBuilder newRequestBuilder(Client client) {
        return new SuggestStatisticsRequestBuilder(client);
    }

    @Override
    public SuggestStatisticsResponse newResponse() {
        return new SuggestStatisticsResponse();
    }
}

package de.spinscale.elasticsearch.client.action.suggest;

import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsAction;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsRequest;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsResponse;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;

public class SuggestStatisticsRequestBuilder extends ActionRequestBuilder<SuggestStatisticsRequest, SuggestStatisticsResponse, SuggestStatisticsRequestBuilder, Client> {

    public SuggestStatisticsRequestBuilder(Client client) {
        super(client, new SuggestStatisticsRequest());
    }

    @Override
    protected void doExecute(ActionListener<SuggestStatisticsResponse> listener) {
        client.execute(SuggestStatisticsAction.INSTANCE, request, listener);
    }
}

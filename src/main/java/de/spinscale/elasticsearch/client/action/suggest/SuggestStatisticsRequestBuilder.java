package de.spinscale.elasticsearch.client.action.suggest;

import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsAction;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsRequest;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsResponse;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

public class SuggestStatisticsRequestBuilder extends ActionRequestBuilder<SuggestStatisticsRequest, SuggestStatisticsResponse, SuggestStatisticsRequestBuilder> {

    public SuggestStatisticsRequestBuilder(Client client) {
        super((InternalGenericClient) client, new SuggestStatisticsRequest());
    }

    @Override
    protected void doExecute(ActionListener<SuggestStatisticsResponse> listener) {
        ((Client)client).execute(SuggestStatisticsAction.INSTANCE, request, listener);
    }
}

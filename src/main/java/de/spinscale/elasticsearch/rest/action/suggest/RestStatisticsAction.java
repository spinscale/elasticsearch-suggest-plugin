package de.spinscale.elasticsearch.rest.action.suggest;

import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsAction;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsRequest;
import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.RestToXContentListener;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestStatisticsAction extends BaseRestHandler {

    @Inject
    public RestStatisticsAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/__suggestStatistics", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        client.execute(SuggestStatisticsAction.INSTANCE,  new SuggestStatisticsRequest(),
                new RestToXContentListener<SuggestStatisticsResponse>(channel));
    }
}

package de.spinscale.elasticsearch.rest.action.suggest;

import de.spinscale.elasticsearch.action.suggest.refresh.SuggestRefreshAction;
import de.spinscale.elasticsearch.action.suggest.refresh.SuggestRefreshRequest;
import de.spinscale.elasticsearch.action.suggest.refresh.SuggestRefreshResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;
import org.elasticsearch.rest.action.support.RestToXContentListener;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.POST;

public class RestRefreshSuggestAction extends BaseRestHandler {

    @Inject public RestRefreshSuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/__suggestRefresh", this);
        controller.registerHandler(POST, "/{index}/__suggestRefresh", this);
        controller.registerHandler(POST, "/{index}/{type}/__suggestRefresh", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, Client client) {
        final String[] indices = Strings.splitStringByCommaToArray(request.param("index"));

        try {
            SuggestRefreshRequest suggestRefreshRequest = new SuggestRefreshRequest(indices);

            if (request.hasContent()) {
                XContentParser parser = XContentFactory.xContent(request.content()).createParser(request.content());
                Map<String, Object> parserMap = parser.mapAndClose();

                if (parserMap.containsKey("field")) {
                    suggestRefreshRequest.field(XContentMapValues.nodeStringValue(parserMap.get("field"), ""));
                }
            }

            client.execute(SuggestRefreshAction.INSTANCE, suggestRefreshRequest, new RestToXContentListener<SuggestRefreshResponse>(channel));
        } catch (IOException e) {
            channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "Could not extract field"));
        }
    }

}

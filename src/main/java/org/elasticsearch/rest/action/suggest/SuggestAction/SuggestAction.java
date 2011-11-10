package org.elasticsearch.rest.action.suggest.SuggestAction;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.service.SuggestService;

public class SuggestAction extends BaseRestHandler {

    @Inject SuggestService suggestService;

    @Inject public SuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/{index}/{type}/_suggest", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String index = request.param("index");

        try {

            // TODO: Get fields in a better way... a more elasticsearch way
            Map<String, Object> jsonMap = JsonXContent.jsonXContent.createParser(request.contentByteArray()).mapAndClose();
            final String field = (String) jsonMap.get("field");
            final String term = (String) jsonMap.get("term");
            final Integer size = Integer.valueOf((String) jsonMap.get("size"));

            if (Strings.isNullOrEmpty(index) || Strings.isNullOrEmpty(field) || Strings.isNullOrEmpty(term)) {
                XContentBuilder errBuilder = restContentBuilder(request)
                    .startObject()
                    .field(new XContentBuilderString("error"), "Please set index, field & term")
                    .endObject();
                channel.sendResponse(new XContentRestResponse(request, INTERNAL_SERVER_ERROR, errBuilder));
            }

            XContentBuilder builder = restContentBuilder(request)
                .startObject()
                .field(new XContentBuilderString("suggest"), suggestService.suggest(index, field, term, size))
                .endObject();

            channel.sendResponse(new XContentRestResponse(request, OK, builder));
        } catch (IOException e) {
            try {
                channel.sendResponse(new XContentThrowableRestResponse(request, e));
            } catch (IOException exc) {
                logger.error("Failed to send failure response", exc);
            }
        }
    }
}

package org.elasticsearch.rest.action.suggest;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.INTERNAL_SERVER_ERROR;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.service.suggest.SuggestService;

public class SuggestAction extends BaseRestHandler {

    @Inject SuggestService suggestService;

    @Inject public SuggestAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/{index}/{type}/_suggest", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        final String[] indices = RestActions.splitIndices(request.param("index"));

        try {

            Map<String, Object> jsonMap = JsonXContent.jsonXContent.createParser(request.contentByteArray()).mapAndClose();
            final String field = XContentMapValues.nodeStringValue(jsonMap.get("field"), "");
            final String term = XContentMapValues.nodeStringValue(jsonMap.get("term"), "");
            final Integer size = XContentMapValues.nodeIntegerValue(jsonMap.get("size"), 10);

            if (indices.length == 0 || Strings.isNullOrEmpty(field) || Strings.isNullOrEmpty(term)) {
                logger.debug("Invalid parameters: indices [{}], field [{}], term [{}]", Arrays.asList(indices).toString(), field, term);
                XContentBuilder errBuilder = restContentBuilder(request)
                    .startObject()
                    .field(new XContentBuilderString("error"), "Please set index, field & term")
                    .endObject();
                channel.sendResponse(new XContentRestResponse(request, INTERNAL_SERVER_ERROR, errBuilder));
            }

            XContentBuilder builder = restContentBuilder(request)
                .startObject()
                .field(new XContentBuilderString("suggest"), suggestService.suggest(indices, field, term, size))
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

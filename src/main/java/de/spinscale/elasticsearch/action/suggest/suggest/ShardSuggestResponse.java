package de.spinscale.elasticsearch.action.suggest.suggest;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class ShardSuggestResponse extends BroadcastShardOperationResponse {

    private List<String> suggestions;

    public ShardSuggestResponse() {}

    public ShardSuggestResponse(String index, int shardId, List<String> suggestions) {
        super(index, shardId);
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return Lists.newArrayList(suggestions);
    }

    public List<String> suggestions() {
        return Lists.newArrayList(suggestions);
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        suggestions = (List<String>) in.readGenericValue();
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeGenericValue(suggestions);
    }
}

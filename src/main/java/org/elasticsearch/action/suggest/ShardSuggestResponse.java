package org.elasticsearch.action.suggest;

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
        int size = in.readVInt();
        suggestions = Lists.newArrayListWithCapacity(size);
        for (int i = 0; i < size; i++) {
            suggestions.add(in.readString());
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(suggestions.size());
        for (String suggestion : suggestions) {
            out.writeString(suggestion);
        }
    }
}

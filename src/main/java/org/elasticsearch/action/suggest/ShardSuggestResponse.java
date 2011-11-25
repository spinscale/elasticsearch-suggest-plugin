package org.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.suggest.SuggestResponse.SuggestItem;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class ShardSuggestResponse extends BroadcastShardOperationResponse {

    private List<SuggestItem> suggestions;

    public ShardSuggestResponse() {}

    public ShardSuggestResponse(String index, int shardId, List<SuggestItem> suggestions) {
        super(index, shardId);
        this.suggestions = suggestions;
    }

    public List<SuggestItem> getSuggestions() {
        return Lists.newArrayList(suggestions);
    }

    public List<SuggestItem> suggestions() {
        return Lists.newArrayList(suggestions);
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        suggestions = new ArrayList<SuggestItem>(size);
        for (int i = 0; i < size; i++) {
            suggestions.add(SuggestItem.readItem(in));
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(suggestions.size());
        for (SuggestItem item : suggestions) {
            item.writeTo(out);
        }
    }
}

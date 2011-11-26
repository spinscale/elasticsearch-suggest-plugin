package org.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;

public class SuggestResponse extends BroadcastOperationResponse {

    private List<SuggestItem> suggestions;

    public SuggestResponse() {
    }

    public SuggestResponse(List<SuggestItem> suggestions, int totalShards, int successfulShards, int failedShards, List<ShardOperationFailedException> shardFailures) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.suggestions = suggestions;
    }

    public List<String> suggestionsAsString() {
        List<String> results = Lists.newArrayList();
        for (SuggestItem item : suggestions) {
            results.add(item.suggest);
        }
        return results;
    }

    public List<SuggestItem> suggestions() {
        return Lists.newArrayList(suggestions);
    }

    public List<SuggestItem>  getSuggestions() {
        return Lists.newArrayList(suggestions);
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        suggestions = Lists.newArrayList();
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

    public static class SuggestItem implements Streamable, Comparable<SuggestItem> {

        public String suggest;

        public SuggestItem() {}

        public SuggestItem(String name) {
            suggest = name;
        }

        public void readFrom(StreamInput in) throws IOException {
            suggest = in.readUTF();
        }

        public void writeTo(StreamOutput out) throws IOException {
            out.writeUTF(suggest);
        }

        public static SuggestItem readItem(StreamInput in) throws IOException {
            SuggestItem item = new SuggestItem();
            item.readFrom(in);
            return item;
        }

        @Override
        public String toString() {
            return suggest;
        }

        @Override
        public int hashCode() {
            return suggest.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return suggest.equals(obj);
        }

        public int compareTo(SuggestItem arg0) {
            return suggest.compareTo(arg0.suggest);
        }
    }
}

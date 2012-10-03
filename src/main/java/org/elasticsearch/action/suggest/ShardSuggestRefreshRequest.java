package org.elasticsearch.action.suggest;

import java.io.IOException;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;


public class ShardSuggestRefreshRequest extends BroadcastShardOperationRequest {

    private String field;

    public ShardSuggestRefreshRequest() {}

    public ShardSuggestRefreshRequest(String index, int shardId, SuggestRefreshRequest request) {
        super(index, shardId);
        field = request.field();
    }

    public String field() {
        return field;
    }

    public void field(String field) {
        this.field = field;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readOptionalString();
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(field);
    }

}

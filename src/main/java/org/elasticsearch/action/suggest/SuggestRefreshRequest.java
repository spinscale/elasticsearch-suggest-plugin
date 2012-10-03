package org.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;


public class SuggestRefreshRequest extends BroadcastOperationRequest {

    private String field;

    public SuggestRefreshRequest() {}

    public SuggestRefreshRequest(String... indices) {
        super(indices);
    }

    public String field() {
        return field;
    }

    public void field(String field) {
        this.field = field;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(field);
    }

    @Override public String toString() {
        return String.format("[%s] field[%s]", Arrays.toString(indices), field);
    }

}

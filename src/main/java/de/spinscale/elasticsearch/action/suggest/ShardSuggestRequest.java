package de.spinscale.elasticsearch.action.suggest;

import java.io.IOException;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class ShardSuggestRequest extends BroadcastShardOperationRequest {

    private int size = 10;
    private String field;
    private float similarity = 1.0f;
    private String term;
    private String[] types = Strings.EMPTY_ARRAY;
    private String suggestType = "fst";

    public ShardSuggestRequest() {}

    public ShardSuggestRequest(String index, int shardId, SuggestRequest request) {
        super(index, shardId, request);
        size = request.size();
        field = request.field();
        term = request.term();
        similarity = request.similarity();
        types = request.types();
        suggestType = request.suggestType();
    }

    public int size() {
        return size;
    }

    public void size(int size) {
        this.size = size;
    }

    public String field() {
        return field;
    }

    public void field(String field) {
        this.field = field;
    }

    public float similarity() {
        return similarity;
    }

    public void similarity(float similarity) {
        this.similarity = similarity;
    }

    public String term() {
        return term;
    }

    public void term(String term) {
        this.term = term;
    }

    public String suggestType() {
        return suggestType;
    }

    public void suggestType(String suggestType) {
        this.suggestType = suggestType;
    }

    public String[] types() {
        return types;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        size = in.readVInt();
        similarity = in.readFloat();
        field = in.readString();
        term = in.readString();
        suggestType = in.readString();
        types = in.readStringArray();
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(size);
        out.writeFloat(similarity);
        out.writeString(field);
        out.writeString(term);
        out.writeString(suggestType);
        out.writeStringArrayNullable(types);
    }

}

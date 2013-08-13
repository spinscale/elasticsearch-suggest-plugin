package de.spinscale.elasticsearch.action.suggest.suggest;

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
    private String queryAnalyzer;
    private String indexAnalyzer;
    private boolean preservePositionIncrements = true;

    public ShardSuggestRequest() {}

    public ShardSuggestRequest(String index, int shardId, SuggestRequest request) {
        super(index, shardId, request);
        size = request.size();
        field = request.field();
        term = request.term();
        similarity = request.similarity();
        types = request.types();
        suggestType = request.suggestType();
        queryAnalyzer = request.queryAnalyzer();
        indexAnalyzer = request.indexAnalyzer();
        preservePositionIncrements = request.preservePositionIncrements();
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

    public String queryAnalyzer() {
        return queryAnalyzer;
    }

    public void queryAnalyzer(String queryAnalyzer) {
        this.queryAnalyzer = queryAnalyzer;
    }

    public String indexAnalyzer() {
        return indexAnalyzer;
    }

    public void indexAnalyzer(String indexAnalyzer) {
        this.indexAnalyzer = indexAnalyzer;
    }

    public boolean preservePositionIncrements() {
        return preservePositionIncrements;
    }

    public void preservePositionIncrements(boolean preservePositionIncrements) {
        this.preservePositionIncrements = preservePositionIncrements;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        size = in.readVInt();
        similarity = in.readFloat();
        field = in.readString();
        term = in.readString();
        suggestType = in.readString();
        queryAnalyzer = in.readOptionalString();
        indexAnalyzer = in.readOptionalString();
        types = in.readStringArray();
        preservePositionIncrements = in.readBoolean();
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(size);
        out.writeFloat(similarity);
        out.writeString(field);
        out.writeString(term);
        out.writeString(suggestType);
        out.writeOptionalString(queryAnalyzer);
        out.writeOptionalString(indexAnalyzer);
        out.writeStringArrayNullable(types);
        out.writeBoolean(preservePositionIncrements);
    }
}

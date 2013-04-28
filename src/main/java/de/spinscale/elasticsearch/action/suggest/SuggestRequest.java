package de.spinscale.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class SuggestRequest extends BroadcastOperationRequest {

    private String[] types = org.elasticsearch.common.Strings.EMPTY_ARRAY;

    private int size = 10;
    private String field;
    private float similarity = 1.0f;
    private String term;
    private String suggestType = "fst";
    private String queryAnalyzer;
    private String indexAnalyzer;

    public SuggestRequest() {
    }

    public SuggestRequest(String... indices) {
        super(indices);
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

    public void suggestType(String suggestType) {
        this.suggestType = suggestType;
    }

    public String suggestType() {
        return suggestType;
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

    @Override public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (field == null || field.length() == 0) {
            validationException = ValidateActions.addValidationError("No suggest field specified", validationException);
        }
        if (term == null || term.length() == 0) {
            validationException = ValidateActions.addValidationError("No query term specified", validationException);
        }
        return validationException;
    }

    String[] types() {
        return types;
    }

    public SuggestRequest types(String... types) {
        this.types = types;
        return this;
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
        out.writeStringArray(types);
    }

    @Override public String toString() {
        return String.format("[%s] %s, field[%s], term[%s], size[%s], similarity[%s], suggestType[%s], indexAnalyzer[%s], queryAnalyzer[%s]",
                Arrays.toString(indices), Arrays.toString(types), field, term, size, similarity, suggestType, indexAnalyzer, queryAnalyzer);
    }

}

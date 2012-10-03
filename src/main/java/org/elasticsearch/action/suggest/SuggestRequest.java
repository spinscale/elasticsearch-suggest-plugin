package org.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.Arrays;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class SuggestRequest extends BroadcastOperationRequest {

    private String[] types = org.elasticsearch.common.Strings.EMPTY_ARRAY;

    private int size = 10;
    private String field;
    private float similarity = 1.0f;
    private String term;

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

    /**
     * Controls the operation threading model.
     */
    @Override public SuggestRequest operationThreading(BroadcastOperationThreading operationThreading) {
        super.operationThreading(operationThreading);
        return this;
    }

    /**
     * Should the listener be called on a separate thread if needed.
     */
    @Override public SuggestRequest listenerThreaded(boolean threadedListener) {
        super.listenerThreaded(threadedListener);
        return this;
    }

    @Override
    public SuggestRequest indices(String... indices) {
        this.indices = indices;
        return this;
    }

    /**
     * The types of documents the query will run against. Defaults to all types.
     */
    String[] types() {
        return types;
    }

    /**
     * The types of documents the query will run against. Defaults to all types.
     */
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

        int typesSize = in.readVInt();
        if (typesSize > 0) {
            types = new String[typesSize];
            for (int i = 0; i < typesSize; i++) {
                types[i] = in.readString();
            }
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);

        out.writeVInt(size);
        out.writeFloat(similarity);
        out.writeString(field);
        out.writeString(term);

        out.writeVInt(types.length);
        for (String type : types) {
            out.writeString(type);
        }
    }

    @Override public String toString() {
        return String.format("[%s] %s, field[%s], term[%s], size[%s], similarity[%s]", Arrays.toString(indices), Arrays.toString(types), field, term, size, similarity);
    }
}

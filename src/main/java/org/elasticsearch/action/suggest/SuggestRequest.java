package org.elasticsearch.action.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.lucene.util.UnicodeUtil;
import org.elasticsearch.ElasticSearchGenerationException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.Actions;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Required;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.Unicode;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;

public class SuggestRequest extends BroadcastOperationRequest {

    private static final XContentType contentType = Requests.CONTENT_TYPE;
    private String[] types = Strings.EMPTY_ARRAY;

    @Nullable protected String routing;

    private byte[] suggestSource;
    private int suggestSourceOffset;
    private int suggestSourceLength;
    private boolean suggestSourceUnsafe;

    SuggestRequest() {
    }

    /**
     * Constructs a new count request against the provided indices. No indices provided means it will
     * run against all indices.
     */
    public SuggestRequest(String... indices) {
        super(indices);
    }

    @Override public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (suggestSource == null) {
            validationException = Actions.addValidationError("suggest query is missing", validationException);
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

    // TODO: FIXME: Do it
    @Override protected void beforeLocalFork() {
        if (suggestSourceUnsafe) {
            suggestSource = Arrays.copyOfRange(suggestSource, suggestSourceOffset, suggestSourceOffset + suggestSourceLength);
            suggestSourceOffset = 0;
            suggestSourceUnsafe = false;
        }
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

    /**
     * The query source to execute.
     *
     * @see org.elasticsearch.index.query.QueryBuilders
     */
    @Required public SuggestRequest query(QueryBuilder queryBuilder) {
        BytesStream bos = queryBuilder.buildAsUnsafeBytes();
        suggestSource = bos.underlyingBytes();
        suggestSourceOffset = 0;
        suggestSourceLength = bos.size();
        suggestSourceUnsafe = true;
        return this;
    }

    /**
     * The query source to execute in the form of a map.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Required public SuggestRequest query(Map querySource) {
        try {
            XContentBuilder builder = XContentFactory.contentBuilder(contentType);
            builder.map(querySource);
            return query(builder);
        } catch (IOException e) {
            throw new ElasticSearchGenerationException("Failed to generate [" + querySource + "]", e);
        }
    }

    @Required public SuggestRequest query(XContentBuilder builder) {
        try {
            suggestSource = builder.underlyingBytes();
            suggestSourceOffset = 0;
            suggestSourceLength = builder.underlyingBytesLength();
            suggestSourceUnsafe = false;
            return this;
        } catch (IOException e) {
            throw new ElasticSearchGenerationException("Failed to generate [" + builder + "]", e);
        }
    }

    /**
     * The query source to execute. It is preferable to use either {@link #query(byte[])}
     * or {@link #query(org.elasticsearch.index.query.QueryBuilder)}.
     */
    @Required public SuggestRequest query(String querySource) {
        UnicodeUtil.UTF8Result result = Unicode.fromStringAsUtf8(querySource);
        suggestSource = result.result;
        suggestSourceOffset = 0;
        suggestSourceLength = result.length;
        suggestSourceUnsafe = true;
        return this;
    }

    /**
     * The query source to execute.
     */
    @Required public SuggestRequest query(byte[] suggestSource) {
        return query(suggestSource, 0, suggestSource.length, false);
    }

    /**
     * The query source to execute.
     */
    @Required public SuggestRequest query(byte[] querySource, int offset, int length, boolean unsafe) {
        suggestSource = querySource;
        suggestSourceOffset = offset;
        suggestSourceLength = length;
        suggestSourceUnsafe = unsafe;
        return this;
    }

    public byte[] querySource() {
        return suggestSource;
    }

    public int querySourceOffset() {
        return suggestSourceOffset;
    }

    public int querySourceLength() {
        return suggestSourceLength;
    }

    public String routing() {
        return routing;
    }

    public SuggestRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    public SuggestRequest routing(String... routings) {
        routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);

        if (in.readBoolean()) {
            routing = in.readUTF();
        }

        suggestSourceOffset = 0;
        suggestSourceLength = in.readVInt();
        suggestSource = new byte[suggestSourceLength];
        in.readFully(suggestSource);

        int typesSize = in.readVInt();
        if (typesSize > 0) {
            types = new String[typesSize];
            for (int i = 0; i < typesSize; i++) {
                types[i] = in.readUTF();
            }
        }
    }

    @Override public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);

        if (routing == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeUTF(routing);
        }

        out.writeVInt(suggestSourceLength);
        out.writeBytes(suggestSource, suggestSourceOffset, suggestSourceLength);

        out.writeVInt(types.length);
        for (String type : types) {
            out.writeUTF(type);
        }
    }

    @Override public String toString() {
        return "[" + Arrays.toString(indices) + "]" + Arrays.toString(types) + ", querySource[" + Unicode.fromBytes(suggestSource) + "]";
    }
}

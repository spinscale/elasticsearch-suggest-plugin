package org.elasticsearch.action.suggest;

import java.io.IOException;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class ShardSuggestRequest extends BroadcastShardOperationRequest {

    private byte[] querySource;
    private int querySourceOffset;
    private int querySourceLength;

    private String[] types = Strings.EMPTY_ARRAY;

    public ShardSuggestRequest() {}

    public ShardSuggestRequest(String index, int shardId, SuggestRequest request) {
        super(index, shardId);
        querySource = request.querySource();
        querySourceOffset = request.querySourceOffset();
        querySourceLength = request.querySourceLength();
        types = request.types();
    }

    public byte[] querySource() {
        return querySource;
    }

    public int querySourceOffset() {
        return querySourceOffset;
    }

    public int querySourceLength() {
        return querySourceLength;
    }

    public String[] types() {
        return types;
    }

    @Override public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        querySourceLength = in.readVInt();
        querySourceOffset = 0;
        querySource = new byte[querySourceLength];
        in.readFully(querySource);
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
        out.writeVInt(querySourceLength);
        out.writeBytes(querySource, querySourceOffset, querySourceLength);
        out.writeVInt(types.length);
        for (String type : types) {
            out.writeUTF(type);
        }
    }

}

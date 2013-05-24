package de.spinscale.elasticsearch.action.suggest.statistics;

import de.spinscale.elasticsearch.service.suggest.ShardSuggestService;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.shard.ShardId;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FstStats implements Streamable, Serializable, ToXContent {

    private List<FstIndexShardStats> stats = Lists.newArrayList();

    public List<FstIndexShardStats> getStats() {
        return stats;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        long size = in.readLong();
        for (int i = 0 ; i < size; i++) {
            FstIndexShardStats fstIndexShardStats = new FstIndexShardStats();
            fstIndexShardStats.readFrom(in);
            stats.add(fstIndexShardStats);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(stats.size());
        for (FstIndexShardStats fstIndexShardStats : stats) {
            fstIndexShardStats.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startArray("fstStats");

        for (FstIndexShardStats fstIndexShardStats : stats) {
            fstIndexShardStats.toXContent(builder, params);
        }

        builder.endArray();

        return builder;
    }


    public static class FstIndexShardStats implements Streamable, Serializable, ToXContent {

        private ShardId shardId;
        private String type;
        private ShardSuggestService.FieldType fieldType;
        private long sizeInBytes;

        public FstIndexShardStats() {}

        public FstIndexShardStats(ShardId shardId, String type, ShardSuggestService.FieldType fieldType, long sizeInBytes) {
            this.shardId = shardId;
            this.type = type;
            this.fieldType = fieldType;
            this.sizeInBytes = sizeInBytes;
        }

        public ShardId getShardId() {
            return shardId;
        }

        public String getType() {
            return type;
        }

        public ShardSuggestService.FieldType getFieldType() {
            return fieldType;
        }

        public long getSizeInBytes() {
            return sizeInBytes;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            type = in.readString();
            sizeInBytes = in.readLong();
            shardId = ShardId.readShardId(in);
            fieldType = new ShardSuggestService.FieldType();
            fieldType.readFrom(in);

        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(type);
            out.writeLong(sizeInBytes);
            shardId.writeTo(out);
            fieldType.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("index", shardId.getIndex());
            builder.field("id", shardId.getId());
            builder.field("sizeInBytes", sizeInBytes);
            builder.field("type", type);
            fieldType.toXContent(builder, params);
            builder.endObject();
            return builder;
        }
    }
}

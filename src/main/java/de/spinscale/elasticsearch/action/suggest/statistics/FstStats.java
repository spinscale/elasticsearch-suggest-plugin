package de.spinscale.elasticsearch.action.suggest.statistics;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FstStats implements Streamable, Serializable, ToXContent {

    private Map<String, List<FstIndexShardStats>> stats = Maps.newTreeMap();

    public Map<String, List<FstIndexShardStats>> getStats() {
        return stats;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        String[] entryNames = in.readStringArray();
        if (entryNames.length > 0) {
            for (int i = 0 ; i < entryNames.length; i++) {
                long shardSize = in.readLong();

                List<FstIndexShardStats> fstIndexShardStatsList = Lists.newArrayList();
                for (int x = 0 ; x < shardSize ; x++) {
                    FstIndexShardStats fstIndexShardStats = new FstIndexShardStats();
                    fstIndexShardStats.readFrom(in);
                    fstIndexShardStatsList.add(fstIndexShardStats);
                }

                if (fstIndexShardStatsList.size() > 0) {
                    stats.put(entryNames[i], fstIndexShardStatsList);
                }
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeStringArray(stats.keySet().toArray(new String[]{}));
        for (Map.Entry<String, List<FstIndexShardStats>> entries : stats.entrySet()) {
            out.writeLong(entries.getValue().size());
            for (FstIndexShardStats fstIndexShardStats : entries.getValue()) {
                fstIndexShardStats.writeTo(out);
            }
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject("fstStats");

        for (Map.Entry<String, List<FstIndexShardStats>> entry : stats.entrySet()) {
            String indexName = entry.getKey();
            builder.startArray(indexName);
            for (FstIndexShardStats indexShardStats : entry.getValue()) {
                indexShardStats.toXContent(builder, params);
            }
            builder.endArray();
        }

        builder.endObject();

        return builder;
    }


    public static class FstIndexShardStats implements Streamable, Serializable, ToXContent {

        private String fieldName;
        private int shardId;
        private long size;

        public FstIndexShardStats() {}

        public FstIndexShardStats(int shardId, String fieldName, long size) {
            this.shardId = shardId;
            this.fieldName = fieldName;
            this.size = size;
        }

        public String fieldName() {
            return fieldName;
        }

        public int shardId() {
            return shardId;
        }

        public long size() {
            return size;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            fieldName = in.readString();
            shardId = in.readInt();
            size = in.readLong();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(fieldName);
            out.writeInt(shardId);
            out.writeLong(size);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(fieldName, size);
            builder.endObject();
            return builder;
        }
    }
}

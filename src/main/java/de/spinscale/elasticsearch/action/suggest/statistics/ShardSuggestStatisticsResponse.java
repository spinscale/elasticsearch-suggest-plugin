package de.spinscale.elasticsearch.action.suggest.statistics;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.shard.ShardId;

import java.io.IOException;
import java.util.List;

public class ShardSuggestStatisticsResponse extends BroadcastShardOperationResponse {

    private List<FstStats.FstIndexShardStats> shardStats = Lists.newArrayList();

    public ShardSuggestStatisticsResponse() {}

    public ShardSuggestStatisticsResponse(ShardId shardId) {
        super(shardId.getIndex(), shardId.id());
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        long size = in.readLong();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                FstStats.FstIndexShardStats fstIndexShardStats = new FstStats.FstIndexShardStats();
                fstIndexShardStats.readFrom(in);
                shardStats.add(fstIndexShardStats);
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(shardStats.size());
        for (FstStats.FstIndexShardStats fstIndexShardStats : shardStats) {
            fstIndexShardStats.writeTo(out);
        }
    }

    public List<FstStats.FstIndexShardStats> getFstIndexShardStats() {
        return shardStats;
    }
}

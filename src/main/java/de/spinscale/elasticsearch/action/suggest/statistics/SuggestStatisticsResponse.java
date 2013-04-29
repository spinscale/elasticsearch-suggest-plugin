package de.spinscale.elasticsearch.action.suggest.statistics;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SuggestStatisticsResponse extends BroadcastOperationResponse {

    private FstStats fstStats = new FstStats();

    public SuggestStatisticsResponse() {}

    public SuggestStatisticsResponse(int totalShards, int successfulShards, int failedShards,
                                     List<ShardSuggestStatisticsResponse> successfulStatistics,
                                     List<ShardOperationFailedException> shardFailures) {
        super(totalShards, successfulShards, failedShards, shardFailures);

        for (ShardSuggestStatisticsResponse response : successfulStatistics) {
            String index = response.getIndex();
            int shardId = response.getShardId();
            if (Strings.hasLength(index) && response.getFstIndexShardStats() != null && response.getFstIndexShardStats().size() > 0) {
                fstStats.getStats().put(index + "-" + shardId, response.getFstIndexShardStats());
            }
        }
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        fstStats = new FstStats();
        fstStats.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        fstStats.writeTo(out);
    }

    public FstStats fstStats() {
        return fstStats;
    }
    public FstStats getFstStats() {
        return fstStats;
    }
}

package de.spinscale.elasticsearch.action.suggest.statistics;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

public class SuggestStatisticsResponse extends BroadcastOperationResponse implements ToXContent {

    private FstStats fstStats = new FstStats();

    public SuggestStatisticsResponse() {}

    public SuggestStatisticsResponse(int totalShards, int successfulShards, int failedShards,
                                     List<ShardSuggestStatisticsResponse> successfulStatistics,
                                     List<ShardOperationFailedException> shardFailures) {
        super(totalShards, successfulShards, failedShards, shardFailures);

        for (ShardSuggestStatisticsResponse response : successfulStatistics) {
            if (response.getFstIndexShardStats() != null && response.getFstIndexShardStats().size() > 0) {
                fstStats.getStats().addAll(response.getFstIndexShardStats());
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

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        buildBroadcastShardsHeader(builder, this);
        fstStats.toXContent(builder, params);
        return builder;
    }
}

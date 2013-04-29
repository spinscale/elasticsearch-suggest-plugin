package de.spinscale.elasticsearch.action.suggest.statistics;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;

public class ShardSuggestStatisticsRequest extends BroadcastShardOperationRequest {

    public ShardSuggestStatisticsRequest() {
    }

    public ShardSuggestStatisticsRequest(String index, int shardId, SuggestStatisticsRequest request) {
        super(index, shardId, request);
    }

}

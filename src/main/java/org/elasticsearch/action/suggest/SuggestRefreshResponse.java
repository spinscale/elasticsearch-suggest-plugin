package org.elasticsearch.action.suggest;

import java.util.List;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;


public class SuggestRefreshResponse extends BroadcastOperationResponse {

    public SuggestRefreshResponse() {}

    public SuggestRefreshResponse(int totalShards, int successfulShards, int failedShards, List<ShardOperationFailedException> shardFailures) {
        super(totalShards, successfulShards, failedShards, shardFailures);
    }

}

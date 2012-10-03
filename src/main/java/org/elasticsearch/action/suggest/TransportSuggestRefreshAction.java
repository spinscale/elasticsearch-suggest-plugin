package org.elasticsearch.action.suggest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.service.suggest.ShardSuggestService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;


public class TransportSuggestRefreshAction extends TransportBroadcastOperationAction<SuggestRefreshRequest, SuggestRefreshResponse, ShardSuggestRefreshRequest, ShardSuggestRefreshResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportSuggestRefreshAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService, IndicesService indicesService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
    }

    @Override
    protected String transportAction() {
        return SuggestRefreshAction.NAME;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected SuggestRefreshRequest newRequest() {
        return new SuggestRefreshRequest();
    }

    @Override
    protected SuggestRefreshResponse newResponse(SuggestRefreshRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = Lists.newArrayList();

        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse == null) {
                failedShards++;
            } else if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                successfulShards++;
            }
        }

        return new SuggestRefreshResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures);
    }

    @Override
    protected ShardSuggestRefreshRequest newShardRequest() {
        return new ShardSuggestRefreshRequest();
    }

    @Override
    protected ShardSuggestRefreshRequest newShardRequest(ShardRouting shard, SuggestRefreshRequest request) {
        return new ShardSuggestRefreshRequest(shard.index(), shard.id(), request);
    }

    @Override
    protected ShardSuggestRefreshResponse newShardResponse() {
        return new ShardSuggestRefreshResponse();
    }

    @Override
    protected ShardSuggestRefreshResponse shardOperation(ShardSuggestRefreshRequest request) throws ElasticSearchException {
        logger.trace("Entered TransportSuggestRefreshAction.shardOperation()");
        IndexService indexService = indicesService.indexServiceSafe(request.index());
        ShardSuggestService suggestShardService = indexService.shardInjectorSafe(request.shardId()).getInstance(ShardSuggestService.class);
        return suggestShardService.refresh(request);
    }

    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, SuggestRefreshRequest request, String[] concreteIndices) {
        return clusterService.operationRouting().searchShards(clusterState, request.indices(), concreteIndices, null, null, null);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, SuggestRefreshRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, SuggestRefreshRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA, concreteIndices);
    }
}

package de.spinscale.elasticsearch.action.suggest.statistics;

import de.spinscale.elasticsearch.service.suggest.ShardSuggestService;
import org.elasticsearch.ElasticsearchException;
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
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TransportSuggestStatisticsAction extends TransportBroadcastOperationAction<SuggestStatisticsRequest, SuggestStatisticsResponse, ShardSuggestStatisticsRequest, ShardSuggestStatisticsResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportSuggestStatisticsAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                            TransportService transportService, IndicesService indicesService) {
        super(settings, SuggestStatisticsAction.NAME, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected SuggestStatisticsRequest newRequest() {
        return new SuggestStatisticsRequest();
    }

    @Override
    protected SuggestStatisticsResponse newResponse(SuggestStatisticsRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = Lists.newArrayList();
        List<ShardSuggestStatisticsResponse> successfulStatistics = Lists.newArrayList();

        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse == null) {
                failedShards++;
            } else if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                successfulShards++;
                successfulStatistics.add((ShardSuggestStatisticsResponse)shardResponse);
            }
        }

        return new SuggestStatisticsResponse(shardsResponses.length(), successfulShards, failedShards, successfulStatistics, shardFailures);
    }

    @Override
    protected ShardSuggestStatisticsRequest newShardRequest() {
        return new ShardSuggestStatisticsRequest();
    }

    @Override
    protected ShardSuggestStatisticsRequest newShardRequest(int numShards, ShardRouting shard, SuggestStatisticsRequest request) {
        return new ShardSuggestStatisticsRequest(shard.index(), shard.id(), request);
    }

    @Override
    protected ShardSuggestStatisticsResponse newShardResponse() {
        return new ShardSuggestStatisticsResponse();
    }

    @Override
    protected ShardSuggestStatisticsResponse shardOperation(ShardSuggestStatisticsRequest request) throws ElasticsearchException {
        IndexService indexService = indicesService.indexServiceSafe(request.index());
        ShardSuggestService suggestShardService = indexService.shardInjectorSafe(request.shardId()).getInstance(ShardSuggestService.class);
        return suggestShardService.getStatistics();
    }

    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, SuggestStatisticsRequest request, String[] concreteIndices) {
        return clusterService.operationRouting().searchShards(clusterState, request.indices(), concreteIndices, null, null);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, SuggestStatisticsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, SuggestStatisticsRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA, concreteIndices);
    }
}

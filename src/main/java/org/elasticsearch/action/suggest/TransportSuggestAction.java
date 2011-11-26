package org.elasticsearch.action.suggest;

import static org.elasticsearch.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.collect.ImmutableSortedSet;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.service.suggest.SuggestService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportSuggestAction extends TransportBroadcastOperationAction<SuggestRequest, SuggestResponse, ShardSuggestRequest, ShardSuggestResponse> {

    private IndicesService indicesService;
    private SuggestService suggestService;

    @Inject public TransportSuggestAction(Settings settings, ThreadPool threadPool,
            ClusterService clusterService, TransportService transportService,
            IndicesService indicesService, SuggestService suggestService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
        this.suggestService = suggestService;
    }

    @Override
    protected String transportAction() {
        return "indices/suggest";
    }

    @Override
    protected String transportShardAction() {
        return "indices/suggest/shard";
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SEARCH;
    }

    @Override
    protected SuggestRequest newRequest() {
        return new SuggestRequest();
    }

    @Override
    protected SuggestResponse newResponse(SuggestRequest request,
            AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        System.out.println("Entered TransportSuggestAction.newResponse()");

        // I am parsing this here, because otherwise
        int size = 10;
        try {
            XContentParser parser = XContentFactory.xContent(request.querySource()).createParser(request.querySource());
            Map<String, Object> parserMap = parser.mapAndClose();
            size = XContentMapValues.nodeIntegerValue(parserMap.get("size"), 10);
        } catch (Exception e) {}

        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;
        List<String> items = Lists.newArrayList();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse == null) {
                failedShards++;
            } else if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newArrayList();
                }
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else if (shardResponse instanceof ShardSuggestResponse) {
                ShardSuggestResponse shardSuggestResponse = (ShardSuggestResponse) shardResponse;
                List<String> shardItems = shardSuggestResponse.suggestions();
                items.addAll(shardItems);
                successfulShards++;
            } else {
                successfulShards++;
            }
        }

        List<String> resultItems = ImmutableSortedSet.copyOf(items).asList();
        return new SuggestResponse(resultItems.subList(0, Math.min(resultItems.size(), size)),
                shardsResponses.length(), successfulShards, failedShards, shardFailures);
    }

    @Override
    protected ShardSuggestRequest newShardRequest() {
        return new ShardSuggestRequest();
    }

    @Override
    protected ShardSuggestRequest newShardRequest(ShardRouting shard,
            SuggestRequest request) {
        return new ShardSuggestRequest(shard.index(), shard.id(), request);
    }

    @Override
    protected ShardSuggestResponse newShardResponse() {
        return new ShardSuggestResponse();
    }

    @Override
    protected ShardSuggestResponse shardOperation(ShardSuggestRequest request) throws ElasticSearchException {
        System.out.println("Entered TransportSuggestAction.shardOperation()");
        IndexShard indexShard = indicesService.indexServiceSafe(request.index()).shardSafe(request.shardId());
        List<String> items = suggestService.suggest(indexShard, request.querySource());
        return new ShardSuggestResponse(request.index(), request.shardId(), items);
    }

    @Override
    protected GroupShardsIterator shards(SuggestRequest request,
            String[] concreteIndices, ClusterState clusterState) {
        System.out.println("Entered TransportSuggestAction.shards()");
        return clusterService.operationRouting().searchShards(clusterState, request.indices(), concreteIndices, null, null, null);
    }

}

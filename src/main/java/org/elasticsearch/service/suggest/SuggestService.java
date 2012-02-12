package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.ShardSuggestRequest;
import org.elasticsearch.action.suggest.TransportNodesSuggestRefreshAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.index.shard.service.IndexShard;

public class SuggestService extends AbstractLifecycleComponent<SuggestService> {

    private TimeValue suggestRefreshInterval;
    private volatile Thread suggestUpdaterThread;
    private volatile boolean closed;
    private TransportNodesSuggestRefreshAction suggestRefreshAction;
    private Suggester suggester;
    private ClusterService clusterService;

    @Inject public SuggestService(Settings settings, Suggester suggester, TransportNodesSuggestRefreshAction suggestRefreshAction,
            ClusterService clusterService) {
        super(settings);
        this.suggester = suggester;
        this.suggestRefreshAction = suggestRefreshAction;
        this.clusterService = clusterService;
        suggestRefreshInterval = settings.getAsTime("suggest.refresh_interval", TimeValue.timeValueMinutes(10));
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        logger.info("Suggest component started with refresh interval [{}]", suggestRefreshInterval);
        suggestUpdaterThread = EsExecutors.daemonThreadFactory(settings, "suggest_updater").newThread(new SuggestUpdaterThread());
        suggestUpdaterThread.start();

    }

    @Override
    protected void doStop() throws ElasticSearchException {
        logger.info("Suggest component stopped");
        closeAll();
    }

    @Override
    protected void doClose() throws ElasticSearchException {
        closeAll();
    }

    private void closeAll() {
        if (closed) {
            return;
        }
        closed = true;
        suggester.clean();
        if (suggestUpdaterThread != null) {
            suggestUpdaterThread.interrupt();
        }
    }

    public List<String> suggest(IndexShard indexShard, ShardSuggestRequest request) throws ElasticSearchException {
        return suggest(indexShard, request.field(), request.term(), request.size(), request.similarity());
    }

    public List<String> suggest(IndexShard indexShard, String field, String term, int limit, Float similarity) throws ElasticSearchException {
        try {
            StopWatch shardWatch = new StopWatch().start();

            IndexReader indexReader = indexShard.searcher().searcher().getIndexReader();
            List<String> results = suggester.getSuggestions(indexShard.shardId(), field, term, limit, similarity, indexReader);

            shardWatch.stop();
            if (logger.isDebugEnabled()) {
                logger.debug("Suggested {} results {} for term [{}] in index [{}] shard [{}], duration [{}]", results.size(),
                        results, term, indexShard.shardId().index().name(), indexShard.shardId().id(), shardWatch.totalTime());
            }

            return results;
        } catch (IOException e) {
            throw new ElasticSearchException("Problem with suggest", e);
        } finally {
            indexShard.searcher().release();
        }
    }

    public class SuggestUpdaterThread implements Runnable {
        public void run() {
            while (!closed) {
                DiscoveryNode node = clusterService.localNode();
                DiscoveryNode masterNode = clusterService.state().nodes().masterNode();

                if (node != null && node.equals(masterNode)) {
                    StopWatch sw = new StopWatch().start();
                    suggestRefreshAction.execute(new NodesSuggestRefreshRequest()).actionGet();
                    logger.info("Suggest update took [{}], next update in [{}]", sw.stop().totalTime(), suggestRefreshInterval);
                } else {
                    if (node != null) {
                        logger.debug("[{}]/[{}] is not master node, not triggering update", node.getId(), node.getName());
                    }
                }

                try {
                    Thread.sleep(suggestRefreshInterval.millis());
                } catch (InterruptedException e1) {
                    continue;
                }
            }
        }
    }
}

package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.shard.service.IndexShard;

public class SuggestService extends AbstractLifecycleComponent<SuggestService> {

    private Integer suggestRefreshInterval;
    private Suggester suggester;
    private volatile Thread suggestUpdaterThread;
    private volatile boolean closed;

    @Inject public SuggestService(Settings settings, Suggester suggester) {
        super(settings);
        this.suggester = suggester;
        suggestRefreshInterval = settings.getAsInt("suggest.refresh_interval", 600) * 1000;
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        logger.info("Suggest component started with refresh interval [{}]", TimeValue.timeValueMillis(suggestRefreshInterval));
        // Start indexer thread here
        // index all 5 minutes
        suggestUpdaterThread = EsExecutors.daemonThreadFactory(settings, "suggest_updater").newThread(new SuggestUpdaterThread(suggester));
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

    public List<String> suggest(IndexShard indexShard, byte[] querySource) throws ElasticSearchException {
        try {
            XContentParser parser = XContentFactory.xContent(querySource).createParser(querySource);
            Map<String, Object> parserMap = parser.mapAndClose();
            final String field = XContentMapValues.nodeStringValue(parserMap.get("field"), "");
            final String term = XContentMapValues.nodeStringValue(parserMap.get("term"), "");
            final Integer size = XContentMapValues.nodeIntegerValue(parserMap.get("size"), 10);
            final Float similarity = XContentMapValues.nodeFloatValue(parserMap.get("similarity"), 1.0f);

            logger.trace(String.format("Suggest Query: field %s, term %s, size %s, similarity", field, term, size, similarity));

            return suggest(indexShard, field, term, size, similarity);
        } catch (IOException e) {
            throw new ElasticSearchException("SuggestProblem", e);
        }
    }

    public List<String> suggest(IndexShard indexShard, String field, String term, int limit, Float similarity) throws ElasticSearchException {
        try {
            StopWatch shardWatch = new StopWatch().start();

            IndexReader indexReader = indexShard.searcher().searcher().getIndexReader();
            List<String> results = suggester.getSuggestions(indexShard.shardId(), field, term, limit, similarity, indexReader);

            shardWatch.stop();
            logger.debug("Suggested {} results {} for term [{}] in index [{}] shard [{}], duration [{}]", results.size(),
                    results, term, indexShard.shardId().index().name(), indexShard.shardId().id(), shardWatch.totalTime());

            return results;
        } catch (IOException e) {
            throw new ElasticSearchException("Problem with suggest", e);
        } finally {
            indexShard.searcher().release();
        }
    }

    public class SuggestUpdaterThread implements Runnable {

        private Suggester suggester;

        public SuggestUpdaterThread(Suggester suggester) {
            this.suggester = suggester;
        }

        public void run() {
            while (!closed) {
                StopWatch sw = new StopWatch().start();
                suggester.update();
                logger.info("Suggest update took [{}]", sw.stop().totalTime());

                try {
                    Thread.sleep(suggestRefreshInterval);
                } catch (InterruptedException e1) {
                    continue;
                }
            }
        }
    }
}

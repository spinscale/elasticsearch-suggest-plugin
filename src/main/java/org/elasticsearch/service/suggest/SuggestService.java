package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.fst.FSTLookup;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.suggest.SuggestResponse.SuggestItem;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;

public class SuggestService extends AbstractLifecycleComponent<SuggestService> {

    @Inject private IndicesService indicesService;
    private ConcurrentMap<IndexReader, Map<String, FSTLookup>> lookups = Maps.newConcurrentMap();

    @Inject public SuggestService(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        logger.info("Suggest component started");
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        logger.info("Suggest component stopped");
    }

    @Override
    protected void doClose() throws ElasticSearchException {}

    public List<SuggestItem> suggest(IndexShard indexShard, byte[] querySource) throws ElasticSearchException {
        try {
            XContentParser parser = XContentFactory.xContent(querySource).createParser(querySource);
            Map<String, Object> parserMap = parser.mapAndClose();
            final String field = XContentMapValues.nodeStringValue(parserMap.get("field"), "");
            final String term = XContentMapValues.nodeStringValue(parserMap.get("term"), "");
            final Integer size = XContentMapValues.nodeIntegerValue(parserMap.get("size"), 10);

            System.out.println(String.format("field %s, term %s, size %s", field, term, size));

            return suggest(indexShard, field, term, size);
        } catch (IOException e) {
            throw new ElasticSearchException("SuggestProblem", e);
        }
    }

    public List<SuggestItem> suggest(IndexShard indexShard, String field, String term, int limit) throws ElasticSearchException {
        StopWatch shardWatch = new StopWatch().start();
        try {

            IndexReader indexReader = indexShard.searcher().searcher().getIndexReader();
            FSTLookup lookup;
                lookup = createFSTLookup(indexReader, field);

            List<LookupResult> lookupResults = lookup.lookup(term, true, limit+1); // TODO: Not sure why +1
            shardWatch.stop();
            logger.debug("Suggested {} results {} for term [{}] in index [{}] shard [{}], duration [{}]", lookupResults.size(),
                    lookupResults, term, indexShard.shardId().index().name(), indexShard.shardId().id(), shardWatch.totalTime());

            List<SuggestItem> results = Lists.newArrayList();
            for (LookupResult lookupResult : lookupResults) {
                results.add(new SuggestItem(lookupResult.key));
            }
            return results;

        } catch (IOException e) {
            throw new ElasticSearchException("Problem with suggest", e);
        }
    }


    public List<String> suggest(String[] indices, String field, String term) {
        return suggest(indices, field, term, 10);
    }

    public List<String> suggest(String[] indices, String field, String term, Integer limit) throws ElasticSearchException {
        StopWatch watch = new StopWatch().start();
        IndexReader indexReader = null;

        Collection<String> indexCollection = Arrays.asList(indices);

        try {
            List<LookupResult> lookupResultsOverAllShards = Lists.newArrayList();

            for (String index : indexCollection) {
                if (indicesService.hasIndex(index)) {
                    IndexService indexService = indicesService.indexService(index);
                    Iterator<IndexShard> shardIterator = indexService.iterator();

                    while (shardIterator.hasNext()) {
                        IndexShard indexShard = shardIterator.next();
                        StopWatch shardWatch = new StopWatch().start();

                        indexReader = indexShard.searcher().searcher().getIndexReader();
                        FSTLookup lookup = createFSTLookup(indexReader, field);

                        List<LookupResult> lookupResults = lookup.lookup(term, true, limit+1); // TODO: Not sure why +1
                        lookupResultsOverAllShards.addAll(lookupResults);
                        shardWatch.stop();
                        logger.debug("Suggested {} results {} for term [{}] in index [{}] shard [{}], duration [{}]", lookupResults.size(),
                                lookupResults, term, indexShard.shardId().index().name(), indexShard.shardId().id(), shardWatch.totalTime());
                    }
                }
            }

            Collections.sort(lookupResultsOverAllShards);

            LinkedHashSet<String> results = Sets.newLinkedHashSet();
            for (LookupResult lookupResult : lookupResultsOverAllShards.subList(0, Math.min(limit, lookupResultsOverAllShards.size()))) {
                results.add(lookupResult.key);
            }

            return Lists.newArrayList(results);
        } catch (IOException e) {
            logger.error("Error in suggest service", e);
            throw new ElasticSearchException("Error in suggest", e);
        } finally {
            watch.stop();
            logger.debug("Total suggest time for [{}]: [{}]", term, watch.shortSummary());
        }
    }

    private FSTLookup createFSTLookup(IndexReader indexReader, String field) throws IOException {
        //      SpellChecker checker = new SpellChecker(new RAMDirectory());
        //      checker.indexDictionary(dict);
        if (!lookups.containsKey(indexReader)) {
            Map<String, FSTLookup> emptyLookupMap = Maps.newHashMapWithExpectedSize(2); // Are you doing suggest on more than two fields from your index?
            lookups.putIfAbsent(indexReader, emptyLookupMap);
        }

        if (!lookups.get(indexReader).containsKey(field)) {
            HighFrequencyDictionary dict = new HighFrequencyDictionary(indexReader, field, 0.00001f);
            FSTLookup lookup = new FSTLookup();
            lookup.build(dict);
            lookups.get(indexReader).put(field, lookup);
            logger.debug("Creating FSTLookup for index [{}] and field [{}]", "TBD", field);
        }

        return lookups.get(indexReader).get(field);
    }
}

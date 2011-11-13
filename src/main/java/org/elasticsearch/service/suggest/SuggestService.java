package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.fst.FSTLookup;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
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
                        FSTLookup lookup = createFSTLookup(indexReader, index, field);

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

    private FSTLookup createFSTLookup(IndexReader indexReader, String index, String field) throws IOException {
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
            logger.debug("Creating FSTLookup for index [{}] and field [{}]", index, field);
        }

        return lookups.get(indexReader).get(field);
    }
}

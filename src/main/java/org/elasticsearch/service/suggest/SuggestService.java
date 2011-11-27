package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.fst.FSTLookup;
import org.apache.lucene.store.RAMDirectory;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.StopWatch;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.shard.service.IndexShard;

public class SuggestService extends AbstractLifecycleComponent<SuggestService> {

    private static final ConcurrentMap<IndexReader, Map<String, FSTLookup>> lookups = Maps.newConcurrentMap();
    private static final ConcurrentMap<IndexReader, Map<String, SpellChecker>> spellCheckers = Maps.newConcurrentMap();

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
        StopWatch shardWatch = new StopWatch().start();
        try {

            IndexReader indexReader = indexShard.searcher().searcher().getIndexReader();
            FSTLookup lookup = createFSTLookup(indexReader, indexShard.shardId().index().name(), field);

            List<LookupResult> lookupResults = lookup.lookup(term, true, limit+1); // TODO: Not sure why +1
            shardWatch.stop();
            logger.debug("Suggested {} results {} for term [{}] in index [{}] shard [{}], duration [{}]", lookupResults.size(),
                    lookupResults, term, indexShard.shardId().index().name(), indexShard.shardId().id(), shardWatch.totalTime());

            List<String> results = Lists.newArrayListWithExpectedSize(lookupResults.size());
            for (LookupResult lookupResult : lookupResults) {
                results.add(lookupResult.key);
            }

            // Only check if we want different words
            if (similarity < 1.0f) {
                SpellChecker checker = getSpellChecker(indexReader, indexShard.shardId().index().name(), field);
                String[] suggestSimilar = checker.suggestSimilar(term, limit, similarity);
                results.addAll(Arrays.asList(suggestSimilar));
            }

            return results;

        } catch (IOException e) {
            throw new ElasticSearchException("Problem with suggest", e);
        }
    }

    private FSTLookup createFSTLookup(IndexReader indexReader, String index, String field) throws IOException {
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

    private SpellChecker getSpellChecker(IndexReader indexReader, String index, String field) throws IOException {
        if (!spellCheckers.containsKey(indexReader)) {
            Map<String, SpellChecker> emptyLookupMap = Maps.newHashMapWithExpectedSize(2); // Are you doing suggest on more than two fields from your index?
            spellCheckers.putIfAbsent(indexReader, emptyLookupMap);
        }

        if (!spellCheckers.get(indexReader).containsKey(field)) {
            HighFrequencyDictionary dict = new HighFrequencyDictionary(indexReader, field, 0.00001f);

            SpellChecker checker = new SpellChecker(new RAMDirectory());
            checker.indexDictionary(dict);

            spellCheckers.get(indexReader).put(field, checker);
            logger.debug("Creating Spellchecker for index [{}] and field [{}]", index, field);
        }

        return spellCheckers.get(indexReader).get(field);
    }
}

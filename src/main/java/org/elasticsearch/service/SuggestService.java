package org.elasticsearch.service;

import java.io.IOException;
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
    protected void doStart() throws ElasticSearchException {}

    @Override
    protected void doStop() throws ElasticSearchException {}

    @Override
    protected void doClose() throws ElasticSearchException {}

    public List<String> suggest(String index, String field, String term) {
        return suggest(index, field, term, 10);
    }

    public List<String> suggest(String index, String field, String term, Integer limit) throws ElasticSearchException {
        IndexReader indexReader = null;
        LinkedHashSet<String> results = Sets.newLinkedHashSet();

        if (indicesService.hasIndex(index)) {
            IndexService indexService = indicesService.indexService(index);

            Iterator<IndexShard> shardIterator = indexService.iterator();

            try {
                while (shardIterator.hasNext()) {
                    IndexShard indexShard = shardIterator.next();

                    indexReader = indexShard.searcher().searcher().getIndexReader();
                    FSTLookup lookup;
                    lookup = createFSTLookup(indexReader, field);

                    List<LookupResult> lookupResults = lookup.lookup(term, true, limit+1); // TODO: Not sure why +1
                    for (LookupResult lookupResult : lookupResults) {
                        results.add(lookupResult.key);
                    }
                }
            } catch (IOException e) {
                throw new ElasticSearchException("Error in suggest", e);
            }

        }

        return Lists.newArrayList(results);
    }

    private FSTLookup createFSTLookup(IndexReader indexReader, String field) throws IOException {
        //      SpellChecker checker = new SpellChecker(new RAMDirectory());
        //      checker.indexDictionary(dict);
        if (!lookups.containsKey(indexReader)) {
            Map<String, FSTLookup> emptyLookupMap = Maps.newHashMap();
            lookups.putIfAbsent(indexReader, emptyLookupMap);
        }

        if (!lookups.get(indexReader).containsKey(field)) {
            HighFrequencyDictionary dict = new HighFrequencyDictionary(indexReader, field, 0.00001f);
            FSTLookup lookup = new FSTLookup();
            lookup.build(dict);
            lookups.get(indexReader).put(field, lookup);
        }

        return lookups.get(indexReader).get(field);
    }
}

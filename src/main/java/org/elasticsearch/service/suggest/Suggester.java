package org.elasticsearch.service.suggest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.fst.FSTLookup;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.service.IndexService;
import org.elasticsearch.index.shard.IndexShardState;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;

public class Suggester {

    private ESLogger logger = Loggers.getLogger(getClass());
    private static final ConcurrentMap<String, Structure> structures = Maps.newConcurrentMap();
    private IndicesService indicesService;

    @Inject public Suggester(IndicesService indicesService) {
        this.indicesService = indicesService;
    }

    public List<String> getSuggestions(ShardId shardId, String field, String term, int limit, float similarity,
            IndexReader indexReader) throws IOException {

        String indexField = shardId.index().name() + "/" + shardId.id() + "/" + field;

        Structure structure = structures.get(indexField);
        if (structure == null) {
            structure = Structure.createStructure(field, shardId, indexReader);
            structures.putIfAbsent(indexField, structure);
        }

        List<LookupResult> lookupResults = structure.lookup.lookup(term, true, limit+1);

        List<String> results = Lists.newArrayListWithExpectedSize(lookupResults.size());
        for (LookupResult lookupResult : lookupResults) {
            results.add(lookupResult.key.toString());
        }

        if (similarity < 1.0f) {
            String[] suggestSimilar = structure.spellChecker.suggestSimilar(term, limit, similarity);
            results.addAll(Arrays.asList(suggestSimilar));
        }

        return results;
    }

    public void update() {
        List<Structure> toBeRemoved = Lists.newArrayList();

        for (String indexField : structures.keySet()) {
            Structure structure = structures.get(indexField);

            IndexShard indexShard = null;
            try {
                IndexService indexService = indicesService.indexService(structure.shardId.index().name());
                // No indexService or no indexshard (due to cluster joins/leaves) means removal from here
                if (indexService == null || !indexService.hasShard(structure.shardId.id())) {
                    toBeRemoved.add(structure);
                } else {
                    indexShard = indexService.shardSafe(structure.shardId.id());

                    if (!indexShard.state().equals(IndexShardState.STARTED)) {
                        logger.trace("Index [{}] shard [{}] not started, skipping", indexShard.shardId().index(), indexShard.shardId().id());
                        continue;
                    }

                    Structure refreshedStructure = Structure.createStructure(structure.field, structure.shardId, indexShard.searcher().searcher().getIndexReader());
                    if (refreshedStructure != null) {
                        structures.put(indexField, refreshedStructure);
                    } else {
                        toBeRemoved.add(refreshedStructure);
                    }
                }

            } finally {
                if (indexShard != null && indexShard.searcher() != null) {
                    indexShard.searcher().release();
                }
            }
        }

        for (Structure structure : toBeRemoved) {
            structures.remove(structure);
        }

        logger.trace("Updated [{}] and removed [{}] suggest structures", structures.size(), toBeRemoved.size());
    }

    public static class Structure {
        private static ESLogger logger = Loggers.getLogger(Structure.class);

        public String field;
        public FSTLookup lookup;
        public SpellChecker spellChecker;
        public ShardId shardId;

        public Structure(String field, ShardId shardId, FSTLookup lookup, SpellChecker spellChecker) {
            this.field = field;
            this.lookup = lookup;
            this.spellChecker = spellChecker;
            this.shardId = shardId;
        }

        @Override
        public String toString() {
            return shardId.index().name() + "/" + shardId.id() + "/" + field;
        }

        public static Structure createStructure(String field, ShardId shardId, IndexReader indexReader) {
            try {
                HighFrequencyDictionary dict = new HighFrequencyDictionary(indexReader, field, 0.00001f);

                FSTLookup lookup = new FSTLookup();
                lookup.build(dict);

                SpellChecker spellChecker = new SpellChecker(new RAMDirectory());
                IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, new WhitespaceAnalyzer(Version.LUCENE_35));
                spellChecker.indexDictionary(dict, indexWriterConfig, false);

                return new Structure(field, shardId, lookup, spellChecker);
            } catch (IOException e) {
                logger.error("Error when creating FSTLookup and Spellchecker: [{}]", e, e.getMessage());
            }

            return null;
        }
    }

    public void clean() {
        structures.clear();
    }
}


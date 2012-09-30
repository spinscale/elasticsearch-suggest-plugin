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
import org.apache.lucene.store.AlreadyClosedException;
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

    private final ESLogger logger = Loggers.getLogger(getClass());
    private static final ConcurrentMap<String, Structure> structures = Maps.newConcurrentMap();
    private final IndicesService indicesService;

    @Inject public Suggester(IndicesService indicesService) {
        this.indicesService = indicesService;
    }

    public List<String> getSuggestions(ShardId shardId, String field, String term, int limit, float similarity,
            IndexReader indexReader) throws IOException {

        String indexField = shardId.index().name() + "/" + shardId.id() + "/" + field;

        Structure structure = structures.get(indexField);
        if (structure == null) {
            HighFrequencyDictionary dict = new HighFrequencyDictionary(indexReader, field, 0.00001f);
            structure = Structure.createStructure(field, shardId, dict, indexReader);
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
        List<Structure> toBeRemoved = Lists.newArrayList(structures.values());

        for (String indexField : structures.keySet()) {
            Structure structure = structures.get(indexField);

            IndexShard indexShard = null;
            try {
                IndexService indexService = indicesService.indexService(structure.shardId.index().name());
                // No indexService or no indexshard (due to cluster joins/leaves) means removal from here
                if (indexService != null && indexService.hasShard(structure.shardId.id())) {
                    indexShard = indexService.shardSafe(structure.shardId.id());

                    if (!indexShard.state().equals(IndexShardState.STARTED)) {
                        logger.trace("Index [{}] shard [{}] not started, skipping", indexShard.shardId().index(), indexShard.shardId().id());
                        continue;
                    }

                    HighFrequencyDictionary dict = new HighFrequencyDictionary(indexShard.searcher().reader(), structure.field, 0.00001f);
                    Structure refreshedStructure = Structure.createStructure(structure.field, structure.shardId, dict, indexShard.searcher().reader());
                    if (refreshedStructure != null) {
                        structures.put(indexField, refreshedStructure);
                    }
                }

            } finally {
                if (indexShard != null && indexShard.searcher() != null) {
                    indexShard.searcher().release();
                }
            }
        }

        for (Structure structure : toBeRemoved) {
            if (structures.containsValue(structure)) {
                structures.remove(structure);
            }
            structure.cleanUpResources();
        }

        logger.trace("Updated [{}] and removed [{}] suggest structures", structures.size(), toBeRemoved.size());
    }

    public void clean() {
        for (Structure structure : structures.values()) {
            structure.cleanUpResources();
        }
        structures.clear();
    }

    public static class Structure {
        private static ESLogger logger = Loggers.getLogger(Structure.class);

        public String field;
        public FSTLookup lookup;
        public SpellChecker spellChecker;
        public ShardId shardId;
        public IndexReader indexReader;

        public Structure(String field, ShardId shardId, FSTLookup lookup, SpellChecker spellChecker, IndexReader indexReader) {
            this.field = field;
            this.lookup = lookup;
            this.spellChecker = spellChecker;
            this.shardId = shardId;
            this.indexReader = indexReader;
        }

        public void cleanUpResources() {
            try {
                spellChecker.clearIndex();
            } catch (IOException e) {
                logger.error("Error clearing spellchecker index [{}]: [{}]", e, this, e.getMessage());
            } catch (AlreadyClosedException e) {}

            try {
                spellChecker.close();
            } catch (IOException e) {
                logger.error("Error closing spellchecker index [{}]: [{}]", e, this, e.getMessage());
            } catch (AlreadyClosedException e) {}

            try {
                if (!indexReader.isCurrent()) {
                    IndexReader.openIfChanged(indexReader);
                }
            } catch (IOException e) {
              logger.error("Error reoping index reader [{}]: [{}]", e, this, e.getMessage());
            }

            if (logger.isTraceEnabled()) {
            	logger.trace("Cleaned resources for Index [{}] Shard [{}] Field [{}]",
            			shardId.getIndex(), shardId.getId(), field);
            }
        }

        @Override
        public String toString() {
            return shardId.index().name() + "/" + shardId.id() + "/" + field;
        }

        public static Structure createStructure(String field, ShardId shardId, HighFrequencyDictionary dict, IndexReader indexReader) {
            try {
                FSTLookup lookup = new FSTLookup();
                lookup.build(dict);

                SpellChecker spellChecker = new SpellChecker(new RAMDirectory());
                IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, new WhitespaceAnalyzer(Version.LUCENE_36));
                spellChecker.indexDictionary(dict, indexWriterConfig, false);

                return new Structure(field, shardId, lookup, spellChecker, indexReader);
            } catch (IOException e) {
                logger.error("Error when creating FSTLookup and Spellchecker: [{}]", e, e.getMessage());
            }

            return null;
        }
    }
}


package de.spinscale.elasticsearch.service.suggest;

import de.spinscale.elasticsearch.action.suggest.ShardSuggestRefreshRequest;
import de.spinscale.elasticsearch.action.suggest.ShardSuggestRefreshResponse;
import de.spinscale.elasticsearch.action.suggest.ShardSuggestRequest;
import de.spinscale.elasticsearch.action.suggest.ShardSuggestResponse;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.fst.FSTCompletionLookup;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.base.Function;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.cache.CacheLoader;
import org.elasticsearch.common.cache.LoadingCache;
import org.elasticsearch.common.collect.Collections2;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.shard.service.IndexShard;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ShardSuggestService extends AbstractIndexShardComponent {

    private final IndexShard indexShard;

    private final ReentrantLock lock = new ReentrantLock();
    private IndexReader indexReader;
    private final LoadingCache<String, FSTCompletionLookup> lookupCache;
    private final LoadingCache<String, HighFrequencyDictionary> dictCache;
    private final LoadingCache<String, SpellChecker> spellCheckerCache;
    private final LoadingCache<String, RAMDirectory> ramDirectoryCache;

    @Inject
    public ShardSuggestService(ShardId shardId, @IndexSettings Settings indexSettings, IndexShard indexShard) {
        super(shardId, indexSettings);
        this.indexShard = indexShard;

        ramDirectoryCache = CacheBuilder.newBuilder().build(
                new CacheLoader<String, RAMDirectory>() {
                    @Override
                    public RAMDirectory load(String field) throws Exception {
                        return new RAMDirectory();
                    }
                }
        );

        dictCache = CacheBuilder.newBuilder().build(
                new CacheLoader<String, HighFrequencyDictionary>() {
                    @Override
                    public HighFrequencyDictionary load(String field) throws Exception {
                        return new HighFrequencyDictionary(createOrGetIndexReader(), field, 0.00001f);
                    }
                }
        );

        spellCheckerCache = CacheBuilder.newBuilder().build(
                new CacheLoader<String, SpellChecker>() {
                    @Override
                    public SpellChecker load(String field) throws Exception {
                        SpellChecker spellChecker = new SpellChecker(ramDirectoryCache.get(field));
                        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_41, new WhitespaceAnalyzer(Version.LUCENE_41));
                        spellChecker.indexDictionary(dictCache.getUnchecked(field), indexWriterConfig, false);
                        return spellChecker;
                    }
                }
        );

        lookupCache = CacheBuilder.newBuilder().build(
                new CacheLoader<String, FSTCompletionLookup>() {
                    @Override
                    public FSTCompletionLookup load(String field) throws Exception {
                        FSTCompletionLookup lookup = new FSTCompletionLookup();
                        lookup.build(dictCache.getUnchecked(field));
                        return lookup;
                    }
                }
        );
    }

    public ShardSuggestRefreshResponse refresh(ShardSuggestRefreshRequest shardSuggestRefreshRequest) {
        String field = shardSuggestRefreshRequest.field();
        if (field == null || field.length() == 0) {
            update();
        } else {
            resetIndexReader();

            HighFrequencyDictionary dict = dictCache.getIfPresent(field);
            if (dict != null) dictCache.refresh(field);

            RAMDirectory ramDirectory = ramDirectoryCache.getIfPresent(field);
            if (ramDirectory != null) {
                ramDirectory.close();
                ramDirectoryCache.invalidate(field);
            }

            SpellChecker spellChecker = spellCheckerCache.getIfPresent(field);
            if (spellChecker != null) {
                spellCheckerCache.refresh(field);
                try {
                    spellChecker.close();
                } catch (IOException e) {
                    logger.error("Could not close spellchecker in indexshard [{}] for field [{}]", e, indexShard, field);
                }
            }

            FSTCompletionLookup lookup = lookupCache.getIfPresent(field);
            if (lookup != null) lookupCache.refresh(field);
        }

        return new ShardSuggestRefreshResponse(shardId.index().name(), shardId.id());
    }

    public void shutDown() {
        resetIndexReader();
        dictCache.invalidateAll();
        for (Map.Entry<String, SpellChecker> entry : spellCheckerCache.asMap().entrySet()) {
            try {
                ramDirectoryCache.getUnchecked(entry.getKey()).close();
                entry.getValue().close();
            } catch (IOException e) {
                logger.error("Could not close spellchecker in indexshard [{}] for field [{}]", e, indexShard, entry.getKey());
            }
        }
        spellCheckerCache.invalidateAll();
        ramDirectoryCache.invalidateAll();
        lookupCache.invalidateAll();
    }

    public void update() {
        resetIndexReader();

        for (String field : dictCache.asMap().keySet()) {
            dictCache.refresh(field);
        }

        try {
            for (String field : spellCheckerCache.asMap().keySet()) {
                SpellChecker oldSpellchecker = spellCheckerCache.getUnchecked(field);
                RAMDirectory oldRamDirectory = ramDirectoryCache.getUnchecked(field);
                ramDirectoryCache.refresh(field);
                spellCheckerCache.refresh(field);
                oldRamDirectory.close();
                oldSpellchecker.close();
            }
        } catch (IOException e ) {
            logger.error("Error refreshing spell checker cache [{}]", e, shardId);
        }

        for (String field : lookupCache.asMap().keySet()) {
            lookupCache.refresh(field);
        }
    }

    public ShardSuggestResponse suggest(ShardSuggestRequest shardSuggestRequest) {
        String field = shardSuggestRequest.field();
        String term = shardSuggestRequest.term();
        int limit = shardSuggestRequest.size();

        List<String> suggestions = Lists.newArrayList(getSuggestions(field, term, limit));

        float similarity = shardSuggestRequest.similarity();
        if (similarity < 1.0f && suggestions.size() < limit) {
            suggestions.addAll(getSimilarSuggestions(field, term, limit, similarity));
        }

        return new ShardSuggestResponse(shardId.index().name(), shardId.id(), suggestions);
    }

    private Collection<String> getSimilarSuggestions(String field, String term, int limit, float similarity) {
        try {
            String[] suggestSimilar = spellCheckerCache.getUnchecked(field).suggestSimilar(term, limit, similarity);
            return Arrays.asList(suggestSimilar);
        } catch (IOException e) {
            logger.error("Error getting spellchecker suggestions for shard [{}] field [{}] term [{}] limit [{}] similarity [{}]", e, shardId, field, term, limit, similarity);
        }

        return Collections.emptyList();
    }

    private Collection<String> getSuggestions(String field, String term, int limit) {
        List<LookupResult> lookupResults = lookupCache.getUnchecked(field).lookup(term, true, limit + 1);
        return Collections2.transform(lookupResults, new LookupResultToStringFunction());
    }

    private class LookupResultToStringFunction implements Function<LookupResult, String> {
        @Override
        public String apply(LookupResult result) {
            return result.key.toString();
        }
    }

    public void resetIndexReader() {
        Engine.Searcher currentIndexSearcher = indexShard.searcher();
        IndexReader currentIndexReader = currentIndexSearcher.reader();
        currentIndexSearcher.release();

        // if this index reader is now used in the current index searcher, we need to decrease the old refcount
        if (indexReader != null && indexReader.getRefCount() > 0 && !indexReader.equals(currentIndexReader)) {
            try {
                indexReader.decRef();
            } catch (IOException e) {
                logger.error("Error decreasing indexreader ref count [{}] of shard [{}]", e, indexReader.getRefCount(), shardId);
            }
        }

        indexReader = null;
    }

    // this does not look thread safe and nice...
    private IndexReader createOrGetIndexReader() {
        try {
            if (indexReader == null) {
                lock.lock();
                if (indexReader == null) {
                    // logger.info("1 shard {} : ref count {}", shardId, indexReader.getRefCount());
                    Engine.Searcher indexSearcher = indexShard.searcher();
                    indexReader = indexSearcher.reader();
                    logger.info("2 shard {} : ref count {} with indexReader {}", shardId, indexReader.getRefCount(), indexReader);
                    boolean result = indexSearcher.release();
                    logger.info("3 shard {} : ref count {}  with indexReader {} and result {}", shardId, indexReader.getRefCount(), indexReader, result);
                }
            }
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }

        return indexReader;
    }
}

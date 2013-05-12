package de.spinscale.elasticsearch.service.suggest;

import de.spinscale.elasticsearch.action.suggest.refresh.ShardSuggestRefreshRequest;
import de.spinscale.elasticsearch.action.suggest.refresh.ShardSuggestRefreshResponse;
import de.spinscale.elasticsearch.action.suggest.statistics.FstStats;
import de.spinscale.elasticsearch.action.suggest.statistics.ShardSuggestStatisticsResponse;
import de.spinscale.elasticsearch.action.suggest.suggest.ShardSuggestRequest;
import de.spinscale.elasticsearch.action.suggest.suggest.ShardSuggestResponse;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.search.suggest.fst.FSTCompletionLookup;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.base.Function;
import org.elasticsearch.common.base.Joiner;
import org.elasticsearch.common.cache.CacheBuilder;
import org.elasticsearch.common.cache.CacheLoader;
import org.elasticsearch.common.cache.LoadingCache;
import org.elasticsearch.common.collect.Collections2;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.IndexShardState;
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
    private final LoadingCache<FieldType, AnalyzingSuggester> analyzingSuggesterCache;
    private final LoadingCache<FieldType, FuzzySuggester> fuzzySuggesterCache;
    private final LoadingCache<String, HighFrequencyDictionary> dictCache;
    private final LoadingCache<String, SpellChecker> spellCheckerCache;
    private final LoadingCache<String, RAMDirectory> ramDirectoryCache;

    @Inject
    public ShardSuggestService(ShardId shardId, @IndexSettings Settings indexSettings, IndexShard indexShard,
                               final AnalysisService analysisService, final MapperService mapperService) {
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

        analyzingSuggesterCache = CacheBuilder.newBuilder().build(
                new AbstractCacheLoaderSuggester.CacheLoaderAnalyzingSuggester(mapperService, analysisService, dictCache));

        fuzzySuggesterCache = CacheBuilder.newBuilder().build(
                new AbstractCacheLoaderSuggester.CacheLoaderFuzzySuggester(mapperService, analysisService, dictCache));
    }

    public ShardSuggestRefreshResponse refresh(ShardSuggestRefreshRequest shardSuggestRefreshRequest) {
        String field = shardSuggestRefreshRequest.field();
        if (!Strings.hasLength(field)) {
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

            for (FieldType fieldType : analyzingSuggesterCache.asMap().keySet()) {
                if (fieldType.field().equals(shardSuggestRefreshRequest.field())) {
                    analyzingSuggesterCache.refresh(fieldType);
                }
            }
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
        analyzingSuggesterCache.invalidateAll();
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

        for (FieldType fieldType : analyzingSuggesterCache.asMap().keySet()) {
            analyzingSuggesterCache.refresh(fieldType);
        }
    }

    public ShardSuggestResponse suggest(ShardSuggestRequest shardSuggestRequest) {
        List<String> suggestions = Lists.newArrayList(getSuggestions(shardSuggestRequest));
        return new ShardSuggestResponse(shardId.index().name(), shardId.id(), suggestions);
    }

    private Collection<String> getSimilarSuggestions(ShardSuggestRequest shardSuggestRequest) {
        String field = shardSuggestRequest.field();
        String term = shardSuggestRequest.term();
        Integer limit = shardSuggestRequest.size();
        Float similarity = shardSuggestRequest.similarity();

        try {
            String[] suggestSimilar = spellCheckerCache.getUnchecked(field).suggestSimilar(term, limit, similarity);
            return Arrays.asList(suggestSimilar);
        } catch (IOException e) {
            logger.error("Error getting spellchecker suggestions for shard [{}] field [{}] term [{}] limit [{}] similarity [{}]", e, shardId, field, term, limit, similarity);
        }

        return Collections.emptyList();
    }

    private Collection<String> getSuggestions(ShardSuggestRequest shardSuggestRequest) {
        List<LookupResult> lookupResults = Lists.newArrayList();
        if ("full".equals(shardSuggestRequest.suggestType())) {
            // TODO: support for multiple types here
            lookupResults.addAll(analyzingSuggesterCache.getUnchecked(new FieldType(shardSuggestRequest))
                    .lookup(shardSuggestRequest.term(), false, shardSuggestRequest.size()));

        } else if ("fuzzy".equals(shardSuggestRequest.suggestType())) {
            // TODO: support for multiple types here
            lookupResults.addAll(fuzzySuggesterCache.getUnchecked(new FieldType(shardSuggestRequest))
                    .lookup(shardSuggestRequest.term(), false, shardSuggestRequest.size()));

        } else {
            lookupResults.addAll(lookupCache.getUnchecked(shardSuggestRequest.field())
                    .lookup(shardSuggestRequest.term(), true, shardSuggestRequest.size() + 1));
            Collection<String> suggestions = Collections2.transform(lookupResults, new LookupResultToStringFunction());

            float similarity = shardSuggestRequest.similarity();
            if (similarity < 1.0f && suggestions.size() < shardSuggestRequest.size()) {
                suggestions = Lists.newArrayList(suggestions);
                suggestions.addAll(getSimilarSuggestions(shardSuggestRequest));
            }

            return suggestions;
        }

        return Collections2.transform(lookupResults, new LookupResultToStringFunction());
    }

    private class LookupResultToStringFunction implements Function<LookupResult, String> {
        @Override
        public String apply(LookupResult result) {
            return result.key.toString();
        }
    }

    public void resetIndexReader() {
        IndexReader currentIndexReader = null;
        if (indexShard.state() == IndexShardState.STARTED) {
            Engine.Searcher currentIndexSearcher = indexShard.searcher();
            currentIndexReader = currentIndexSearcher.reader();
            currentIndexSearcher.release();
        }

        // if this index reader is not used in the current index searcher, we need to decrease the old refcount
        if (indexReader != null && indexReader.getRefCount() > 0 && !indexReader.equals(currentIndexReader)) {
            try {
                indexReader.decRef();
            } catch (IOException e) {
                logger.error("Error decreasing indexreader ref count [{}] of shard [{}]", e, indexReader.getRefCount(), shardId);
            }
        }

        indexReader = null;
    }

    public ShardSuggestStatisticsResponse getStatistics() {
        ShardSuggestStatisticsResponse shardSuggestStatisticsResponse = new ShardSuggestStatisticsResponse(shardId());

        for (FieldType fieldType : analyzingSuggesterCache.asMap().keySet()) {
            long sizeInBytes = analyzingSuggesterCache.getIfPresent(fieldType).sizeInBytes();
            FstStats.FstIndexShardStats fstIndexShardStats = new FstStats.FstIndexShardStats(shardId.id(), "analyzingsuggester-" + fieldType, sizeInBytes);
            shardSuggestStatisticsResponse.getFstIndexShardStats().add(fstIndexShardStats);
        }

        for (FieldType fieldType : fuzzySuggesterCache.asMap().keySet()) {
            long sizeInBytes = fuzzySuggesterCache.getIfPresent(fieldType).sizeInBytes();
            FstStats.FstIndexShardStats fstIndexShardStats = new FstStats.FstIndexShardStats(shardId.id(), "fuzzysuggester-" + fieldType, sizeInBytes);
            shardSuggestStatisticsResponse.getFstIndexShardStats().add(fstIndexShardStats);
        }

        return shardSuggestStatisticsResponse;
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
                    indexSearcher.release();

                    // If an indexreader closes, we have to refresh all our data structures!
                    indexReader.addReaderClosedListener(new IndexReader.ReaderClosedListener() {
                        @Override
                        public void onClose(IndexReader reader) {
                            update();
                        }
                    });
                }
            }
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }

        return indexReader;
    }

    public static class FieldType {

        private String field;
        private List<String> types = Lists.newArrayList();
        private String queryAnalyzer;
        private String indexAnalyzer;

        public FieldType(ShardSuggestRequest shardSuggestRequest) {
            this.field = shardSuggestRequest.field();
            this.types = Arrays.asList(shardSuggestRequest.types());
            this.queryAnalyzer = shardSuggestRequest.queryAnalyzer();
            this.indexAnalyzer = shardSuggestRequest.indexAnalyzer();
        }

        public String field() {
            return field;
        }

        public String[] types() {
            return types.toArray(new String[]{});
        }

        public String queryAnalyzer() {
            return queryAnalyzer;
        }

        public String indexAnalyzer() {
            return indexAnalyzer;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(field);
            if (types.size() > 0) {
                sb.append("-types" + Joiner.on("-").join(types));
            }
            if (queryAnalyzer != null && queryAnalyzer.equals(indexAnalyzer)) {
                sb.append("-analyzer:" + queryAnalyzer);
            } else {
                if (queryAnalyzer != null) {
                    sb.append("-queryAnalyzer:" + queryAnalyzer);
                }
                if (indexAnalyzer != null) {
                    sb.append("-indexAnalyzer:" + indexAnalyzer);
                }
            }

            return sb.toString();
        }
    }
}

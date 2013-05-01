package de.spinscale.elasticsearch.service.suggest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.cache.CacheLoader;
import org.elasticsearch.common.cache.LoadingCache;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;

public abstract class AbstractCacheLoaderSuggester<T> extends CacheLoader<ShardSuggestService.FieldType, T> {

    private MapperService mapperService;
    private AnalysisService analysisService;
    protected LoadingCache<String, HighFrequencyDictionary> dictCache;

    public AbstractCacheLoaderSuggester(MapperService mapperService, AnalysisService analysisService,
                                        LoadingCache<String, HighFrequencyDictionary> dictCache) {
        this.mapperService = mapperService;
        this.analysisService = analysisService;
        this.dictCache = dictCache;
    }

    @Override
    public T load(ShardSuggestService.FieldType fieldType) throws Exception {
        FieldMapper fieldMapper = mapperService.smartName(fieldType.field(), fieldType.types()).mapper();

        Analyzer queryAnalyzer = fieldMapper.searchAnalyzer();
        if (fieldType.indexAnalyzer() != null) {
            // TODO: not found case, possible NPE
            queryAnalyzer = analysisService.analyzer(fieldType.queryAnalyzer()).analyzer();
        }
        if (queryAnalyzer == null) {
            queryAnalyzer = new StandardAnalyzer(Version.LUCENE_42);
        }

        Analyzer indexAnalyzer = fieldMapper.searchAnalyzer();
        if (fieldType.indexAnalyzer() != null) {
            // TODO: not found case, possible NPE
            indexAnalyzer = analysisService.analyzer(fieldType.queryAnalyzer()).analyzer();
        }
        if (indexAnalyzer == null) {
            indexAnalyzer = new StandardAnalyzer(Version.LUCENE_42);
        }

        return getSuggester(indexAnalyzer, queryAnalyzer, fieldType);
    }

    public abstract T getSuggester(Analyzer indexAnalyzer, Analyzer queryAnalyzer,
                                   ShardSuggestService.FieldType fieldType) throws Exception;


    public static class CacheLoaderAnalyzingSuggester extends AbstractCacheLoaderSuggester<AnalyzingSuggester> {

        public CacheLoaderAnalyzingSuggester(MapperService mapperService, AnalysisService analysisService, LoadingCache<String, HighFrequencyDictionary> dictCache) {
            super(mapperService, analysisService, dictCache);
        }

        @Override
        public AnalyzingSuggester getSuggester(Analyzer indexAnalyzer, Analyzer queryAnalyzer,
                                               ShardSuggestService.FieldType fieldType) throws Exception {
            AnalyzingSuggester analyzingSuggester = new AnalyzingSuggester(indexAnalyzer, queryAnalyzer);
            analyzingSuggester.build(dictCache.getUnchecked(fieldType.field()));
            return analyzingSuggester;
        }
    }

    public static class CacheLoaderFuzzySuggester extends AbstractCacheLoaderSuggester<FuzzySuggester> {

        public CacheLoaderFuzzySuggester(MapperService mapperService, AnalysisService analysisService, LoadingCache<String, HighFrequencyDictionary> dictCache) {
            super(mapperService, analysisService, dictCache);
        }

        @Override
        public FuzzySuggester getSuggester(Analyzer indexAnalyzer, Analyzer queryAnalyzer,
                                               ShardSuggestService.FieldType fieldType) throws Exception {
            FuzzySuggester fuzzySuggester = new FuzzySuggester(indexAnalyzer, queryAnalyzer);
            fuzzySuggester.build(dictCache.getUnchecked(fieldType.field()));
            return fuzzySuggester;
        }
    }

}

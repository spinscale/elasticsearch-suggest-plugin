package de.spinscale.elasticsearch.service.suggest;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.cache.CacheLoader;
import org.elasticsearch.common.cache.LoadingCache;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.NamedAnalyzer;
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
        MapperService.SmartNameFieldMappers fieldMappers = mapperService.smartName(fieldType.field(), fieldType.types());

        Analyzer queryAnalyzer = null;
        Analyzer indexAnalyzer = null;
        if (fieldMappers != null) {
            FieldMapper fieldMapper = mapperService.smartName(fieldType.field(), fieldType.types()).mapper();

            queryAnalyzer = fieldMapper.searchAnalyzer();
            if (Strings.hasLength(fieldType.indexAnalyzer())) {
                NamedAnalyzer namedAnalyzer = analysisService.analyzer(fieldType.queryAnalyzer());
                if (namedAnalyzer == null) {
                    throw new ElasticsearchException("Query analyzer[" + fieldType.queryAnalyzer() + "] does not exist.");
                }
                queryAnalyzer = namedAnalyzer.analyzer();
            }

            indexAnalyzer = fieldMapper.searchAnalyzer();
            if (Strings.hasLength(fieldType.indexAnalyzer())) {
                NamedAnalyzer namedAnalyzer = analysisService.analyzer(fieldType.indexAnalyzer());
                if (namedAnalyzer == null) {
                    throw new ElasticsearchException("Index analyzer[" + fieldType.indexAnalyzer() + "] does not exist.");
                }
                indexAnalyzer = namedAnalyzer.analyzer();
            }
        }

        if (queryAnalyzer == null) {
            queryAnalyzer = new StandardAnalyzer(org.elasticsearch.Version.CURRENT.luceneVersion);
        }
        if (indexAnalyzer == null) {
            indexAnalyzer = new StandardAnalyzer(org.elasticsearch.Version.CURRENT.luceneVersion);
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
            AnalyzingSuggester analyzingSuggester = new AnalyzingSuggester(indexAnalyzer, queryAnalyzer,
                    AnalyzingSuggester.EXACT_FIRST, 256, -1, fieldType.preservePositionIncrements());
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
            FuzzySuggester fuzzySuggester = new FuzzySuggester(indexAnalyzer, queryAnalyzer, FuzzySuggester.EXACT_FIRST | FuzzySuggester.PRESERVE_SEP, 256, -1,
                    fieldType.preservePositionIncrements(), FuzzySuggester.DEFAULT_MAX_EDITS, FuzzySuggester.DEFAULT_TRANSPOSITIONS,
                    FuzzySuggester.DEFAULT_NON_FUZZY_PREFIX, FuzzySuggester.DEFAULT_MIN_FUZZY_LENGTH, FuzzySuggester.DEFAULT_UNICODE_AWARE);
            fuzzySuggester.build(dictCache.getUnchecked(fieldType.field()));
            return fuzzySuggester;
        }
    }

}

package de.spinscale.elasticsearch.module.suggest.test;

public class SuggestionQuery {

    public final String index;
    public final String type;
    public final String field;
    public final String term;
    public String suggestType;
    public String indexAnalyzer;
    public String queryAnalyzer;
    public Integer size;
    public Float similarity;
    public String analyzer;

    public SuggestionQuery(String index, String type, String field, String term) {
        this.index = index;
        this.type = type;
        this.field = field;
        this.term = term;
    }

    public SuggestionQuery size(Integer size) {
        this.size = size;
        return this;
    }

    public SuggestionQuery similarity(Float similarity) {
        this.similarity = similarity;
        return this;
    }

    public SuggestionQuery suggestType(String suggestType) {
        this.suggestType = suggestType;
        return this;
    }

    public SuggestionQuery analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public SuggestionQuery indexAnalyzer(String indexAnalyzer) {
        this.indexAnalyzer = indexAnalyzer;
        return this;
    }

    public SuggestionQuery queryAnalyzer(String queryAnalyzer) {
        this.queryAnalyzer = queryAnalyzer;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Index [%s] type [%s] field [%s] term [%s]", index, type, field, term));
        if (size != null) sb.append(String.format(" size[%s]", size));
        if (similarity != null) sb.append(String.format(" similarity[%s]", similarity));
        if (suggestType != null) sb.append(String.format(" suggestType[%s]", suggestType));
        if (analyzer != null) sb.append(String.format(" analyzer[%s]", analyzer));
        if (indexAnalyzer!= null) sb.append(String.format(" indexAnalyzer[%s]", indexAnalyzer));
        if (queryAnalyzer != null) sb.append(String.format(" queryAnalyzer[%s]", queryAnalyzer));
        return sb.toString();
    }

}

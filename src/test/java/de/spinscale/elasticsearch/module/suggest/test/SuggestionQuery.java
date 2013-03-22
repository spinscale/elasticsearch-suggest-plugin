package de.spinscale.elasticsearch.module.suggest.test;

public class SuggestionQuery {

    public final String index;
    public final String type;
    public final String field;
    public final String term;
    public String suggestType;
    public Integer size;
    public Float similarity;

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

    @Override
    public String toString() {
        return String.format("Index [%s] type [%s] field [%s] term [%s] size [%s] similarity [%s]", index, type, field, term, size, similarity);
    }

}

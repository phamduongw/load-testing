package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WriteQuery {
    @JsonProperty("type")
    private String type;

    @JsonProperty("connWeight")
    private double connWeight;

    @JsonProperty("queryTemplate")
    private String queryTemplate;

    @JsonProperty("queries")
    private List<WriteQueryDetails> queries;

    public String getType() {
        return type;
    }

    public double getConnWeight() {
        return connWeight;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public List<WriteQueryDetails> getQueries() {
        return queries;
    }
}

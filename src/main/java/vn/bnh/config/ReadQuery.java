package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReadQuery {
    @JsonProperty("connWeight")
    private double connWeight;

    @JsonProperty("queries")
    private List<String> queries;

    public double getConnWeight() {
        return connWeight;
    }

    public List<String> getQueries() {
        return queries;
    }
}

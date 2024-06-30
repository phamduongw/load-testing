package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Transaction {
    @JsonProperty("connWeight")
    private int connWeight;

    @JsonProperty("queries")
    private List<String> queries;

    public int getConnWeight() {
        return connWeight;
    }

    public List<String> getQueries() {
        return queries;
    }
}

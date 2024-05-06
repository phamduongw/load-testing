package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DatabaseConfiguration {
    @JsonProperty("jdbcDriver")
    private String jdbcDriver;

    @JsonProperty("jdbcUrl")
    private String jdbcUrl;

    @JsonProperty("jdbcUsername")
    private String jdbcUsername;

    @JsonProperty("jdbcPassword")
    private String jdbcPassword;

    @JsonProperty("connCount")
    private int connCount;

    @JsonProperty("connPoolingWeight")
    private double connPoolingWeight;

    @JsonProperty("loopCount")
    private LoopCount loopCount;

    @JsonProperty("readQuery")
    private ReadQuery readQuery;

    @JsonProperty("writeQueries")
    private List<WriteQuery> writeQueries;

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public int getConnCount() {
        return connCount;
    }

    public double getConnPoolingWeight() {
        return connPoolingWeight;
    }

    public LoopCount getLoopCount() {
        return loopCount;
    }

    public ReadQuery getReadQuery() {
        return readQuery;
    }

    public List<WriteQuery> getWriteQueries() {
        return writeQueries;
    }
}

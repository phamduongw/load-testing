package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WriteQueryDetails {
    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("targetColumns")
    private List<FieldDetails> targetColumns;

    @JsonProperty("whereClauses")
    private List<FieldDetails> whereClauses;

    public String getTableName() {
        return tableName;
    }

    public List<FieldDetails> getTargetColumns() {
        return targetColumns;
    }

    public List<FieldDetails> getWhereClauses() {
        return whereClauses;
    }
}

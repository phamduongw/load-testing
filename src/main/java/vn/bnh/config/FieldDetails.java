package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldDetails {
    @JsonProperty("name")
    private String name;

    @JsonProperty("sampleValue")
    private String sampleValue;

    @JsonProperty("dataType")
    private String dataType;

    @JsonProperty("isFixed")
    private boolean isFixed;

    public String getName() {
        return name;
    }

    public String getSampleValue() {
        return sampleValue;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isFixed() {
        return isFixed;
    }
}

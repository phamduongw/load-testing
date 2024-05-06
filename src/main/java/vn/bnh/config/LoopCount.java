package vn.bnh.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoopCount {
    @JsonProperty("type")
    private String type;

    @JsonProperty("count")
    private String count;

    public String getType() {
        return type;
    }

    public String getCount() {
        return count;
    }
}

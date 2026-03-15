package acp.cw2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SortedMessage {

    @JsonProperty("Id")
    private int id;

    @JsonProperty("Payload")
    private String payload;

    public SortedMessage() {
    }

    public SortedMessage(int id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    @JsonProperty("Id")
    public int getId() {
        return id;
    }

    @JsonProperty("Id")
    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("Payload")
    public String getPayload() {
        return payload;
    }

    @JsonProperty("Payload")
    public void setPayload(String payload) {
        this.payload = payload;
    }
}
package acp.cw2.dto;

public class TransformMessage {

    private String key;
    private Integer version;
    private Double value;

    public TransformMessage() {
    }

    public TransformMessage(String key, Integer version, Double value) {
        this.key = key;
        this.version = version;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
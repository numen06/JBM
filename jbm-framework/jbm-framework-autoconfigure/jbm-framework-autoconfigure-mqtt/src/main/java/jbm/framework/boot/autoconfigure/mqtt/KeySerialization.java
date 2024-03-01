package jbm.framework.boot.autoconfigure.mqtt;

public class KeySerialization {
    private String clientId;
    private String defaultTopic;

    public KeySerialization() {
        super();
    }

    public KeySerialization(String clientId, String defaultTopic) {
        super();
        this.clientId = clientId;
        this.defaultTopic = defaultTopic;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDefaultTopic() {
        return defaultTopic;
    }

    public void setDefaultTopic(String defaultTopic) {
        this.defaultTopic = defaultTopic;
    }

}
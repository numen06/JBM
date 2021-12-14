package jbm.framework.boot.autoconfigure.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.concurrent.ScheduledExecutorService;

public class SimpleMqttClient extends MqttClient {


    public SimpleMqttClient(String serverURI, String clientId) throws MqttException {
        super(serverURI, clientId);
    }

    public SimpleMqttClient(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
    }

    public SimpleMqttClient(String serverURI, String clientId, MqttClientPersistence persistence, ScheduledExecutorService executorService) throws MqttException {
        super(serverURI, clientId, persistence, executorService);
    }
}

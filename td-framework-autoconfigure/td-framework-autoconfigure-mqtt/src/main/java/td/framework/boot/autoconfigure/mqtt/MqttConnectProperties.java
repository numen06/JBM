package td.framework.boot.autoconfigure.mqtt;

import java.util.Properties;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttConnectProperties {

	private int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
	private int maxInflight = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
	private String willDestination = null;
	private String username;
	private String password;
	private SocketFactory socketFactory;
	private Properties sslClientProps = null;
	private boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
	private int connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
	private String url = null;
	private int MqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;
	private boolean automaticReconnect = false;
	private String clientId = MqttClient.generateClientId();

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	public int getMaxInflight() {
		return maxInflight;
	}

	public void setMaxInflight(int maxInflight) {
		this.maxInflight = maxInflight;
	}

	public String getWillDestination() {
		return willDestination;
	}

	public void setWillDestination(String willDestination) {
		this.willDestination = willDestination;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public Properties getSslClientProps() {
		return sslClientProps;
	}

	public void setSslClientProps(Properties sslClientProps) {
		this.sslClientProps = sslClientProps;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getMqttVersion() {
		return MqttVersion;
	}

	public void setMqttVersion(int mqttVersion) {
		MqttVersion = mqttVersion;
	}

	public boolean isAutomaticReconnect() {
		return automaticReconnect;
	}

	public void setAutomaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

}

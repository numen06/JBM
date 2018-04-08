package td.framework.boot.autoconfigure.tio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="tio.ws.server")
public class AioWsServerProperties {
	private Integer port;
	
	private String ip;

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	
}

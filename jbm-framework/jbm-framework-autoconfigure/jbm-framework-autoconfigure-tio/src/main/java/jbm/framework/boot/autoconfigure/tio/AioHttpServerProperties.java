package jbm.framework.boot.autoconfigure.tio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.tio.http.common.HttpConfig;

@ConfigurationProperties(prefix = "tio.http.server")
public class AioHttpServerProperties {
    private String contextPath;

    private String[] scanPackages;

    private String suffix;

    private Long sessionTimeout = HttpConfig.DEFAULT_SESSION_TIMEOUT;

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

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String[] getScanPackages() {
        return scanPackages;
    }

    public void setScanPackages(String[] scanPackages) {
        this.scanPackages = scanPackages;
    }

}

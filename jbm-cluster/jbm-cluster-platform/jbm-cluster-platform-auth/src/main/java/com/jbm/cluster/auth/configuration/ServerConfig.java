package com.jbm.cluster.auth.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Data
@Slf4j
public class ServerConfig implements ApplicationListener<WebServerInitializedEvent> {

    private Integer serverPort = 5555;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        int port = webServerInitializedEvent.getWebServer().getPort();
        ServerConfigHolder.serverConfig.setServerPort(port);
    }

    private ServerConfig() {
    }

    private static class ServerConfigHolder {
        private static final ServerConfig serverConfig = new ServerConfig();
    }

    public static ServerConfig getInstance() {
        return ServerConfigHolder.serverConfig;
    }
}

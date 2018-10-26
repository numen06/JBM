package jbm.framework.boot.autoconfigure.tio;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.websocket.server.WsServerConfig;
import org.tio.websocket.server.handler.IWsMsgHandler;

@Configuration
@EnableConfigurationProperties({ AioWsServerProperties.class })
@ConditionalOnProperty(prefix = "tio.ws.server", name = "port")
public class AioWsServerAutoConfiguration {

	protected static final Logger logger = LoggerFactory.getLogger(AioWsServerAutoConfiguration.class);

	@Autowired
	private AioWsServerProperties aioWsServerProperties;

	@Bean
	public AioWsServerTemplate WsService(ApplicationContext applicationContext) throws IOException {
		WsServerConfig serverConfig = new WsServerConfig(aioWsServerProperties.getPort());
		IWsMsgHandler wsMsgHandler = applicationContext.getBean(IWsMsgHandler.class);
		AioWsServerTemplate wsServerStarter = new AioWsServerTemplate(serverConfig, wsMsgHandler);
		wsServerStarter.start();
		// wsServerStarter.getServerGroupContext().setHeartbeatTimeout(30 *
		// 1000);
		return wsServerStarter;
	}

}

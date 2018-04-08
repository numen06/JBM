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
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.session.id.ISessionIdGenerator;
import org.tio.http.server.handler.DefaultHttpRequestHandler;
import org.tio.http.server.mvc.Routes;

import jbm.framework.boot.autoconfigure.tio.listener.AioHttpSessionListener;

@Configuration
@EnableConfigurationProperties({ AioHttpServerProperties.class })
@ConditionalOnProperty(prefix = "tio.http.server", name = "port")
public class AioHttpServerAutoConfiguration {

	protected static final Logger logger = LoggerFactory.getLogger(AioHttpServerAutoConfiguration.class);

	@Autowired
	private AioHttpServerProperties aioHttpServerProperties;

	@Bean
	public AioHttpServerTemplate httpServerStarter(ApplicationContext applicationContext) throws IOException {
		AioHttpServerTemplate httpServerStarter;
		HttpConfig httpConfig;
		// String[] scanPackages = new String[] { "com.jbm.test" };// tio

		httpConfig = new HttpConfig(aioHttpServerProperties.getPort(), aioHttpServerProperties.getSessionTimeout(), aioHttpServerProperties.getContextPath(),
			aioHttpServerProperties.getSuffix());
		httpConfig.setSessionIdGenerator(new ISessionIdGenerator() {

			@Override
			public String sessionId(HttpConfig httpConfig, HttpRequest request) {
				return request.getChannelContext().getClientNode().toString();
			}
		});

		Routes routes = new Routes(aioHttpServerProperties.getScanPackages());
		DefaultHttpRequestHandler requestHandler = new DefaultHttpRequestHandler(httpConfig, routes);
		requestHandler.setHttpSessionListener(new AioHttpSessionListener());
		httpServerStarter = new AioHttpServerTemplate(httpConfig, requestHandler);
		httpServerStarter.start();
		return httpServerStarter;
	}

}

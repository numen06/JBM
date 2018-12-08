package jbm.framework.boot.autoconfigure.ssl;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

//@Configuration
//@ConditionalOnProperty(prefix = "server.http", name = "port")
//@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
public class HttpAutoConfiguration {

	@Autowired
	private ConfigurableEnvironment environment;

//	@Bean
//	public EmbeddedServletContainerCustomizer embeddedServletContainerCustomizer() {
//		return new EmbeddedServletContainerCustomizer() {
//			@Override
//			public void customize(ConfigurableEmbeddedServletContainer container) {
//				TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
//				tomcat.addAdditionalTomcatConnectors(initiateHttpConnector());
//			}
//		};
//	}

	private Connector initiateHttpConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setScheme("http");
		connector.setPort(Integer.parseInt(environment.getRequiredProperty("server.http.port")));
		connector.setSecure(false);
		connector.setRedirectPort(Integer.parseInt(environment.resolvePlaceholders("${server.port:8080}")));
		return connector;
	}
}

package td.framework.boot.dubbo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;

import td.framework.boot.dubbo.properties.DubboProperties;

@Configuration
// @ConditionalOnProperty(prefix = DubboProperties.targetName, name = "address")
@EnableConfigurationProperties(DubboProperties.class)
public class DubboAutoConfiguration extends AnnotationBean implements EnvironmentAware {

	private static final Logger logger = LoggerFactory.getLogger(DubboAutoConfiguration.class);

	private static final String APPLICATION_NAME = "dubbo-" + UUID.randomUUID().toString();
	private static final String NAME_PATTERN = "${spring.application.name:${vcap.application.name:${spring.config.name:" + APPLICATION_NAME + "}}}";

	private static final String ANNOTATION_PACKAGE = "com.td";

	public DubboAutoConfiguration() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		super();
	}

	private static final long serialVersionUID = 1L;

	private ConfigurableEnvironment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	public <T> T getPropertiesConfigurationBean(String targetName, Class<T> types) {
		PropertiesConfigurationFactory<T> factory = new PropertiesConfigurationFactory<T>(types);
		factory.setPropertySources(environment.getPropertySources());
		factory.setConversionService(environment.getConversionService());
		factory.setIgnoreInvalidFields(false);
		factory.setIgnoreUnknownFields(true);
		factory.setIgnoreNestedProperties(true);
		factory.setIgnoreNestedProperties(false);
		factory.setTargetName(targetName);
		try {
			factory.bindPropertiesToTarget();
			return factory.getObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		DubboProperties dubboProperties = this.getPropertiesConfigurationBean(DubboProperties.targetName, DubboProperties.class);
		List<ProtocolConfig> protocols = dubboProperties.getProtocols();
		if (protocols == null)
			protocols = new ArrayList<>();
		if (dubboProperties.getProtocol() != null)
			protocols.add(dubboProperties.getProtocol());
		try {
			dubboProperties.afterPropertiesSet();
		} catch (Exception e) {
			logger.error("读取配置错误");
		}
		this.registerThis(dubboProperties.getBasePackage(), beanFactory);
		this.registerApplication(dubboProperties.getApplication(), beanFactory);
		this.registerProtocols(protocols, beanFactory);
		this.registerRegistry(dubboProperties.getRegistry(), beanFactory);
		this.registerMonitor(dubboProperties.getMonitor(), beanFactory);
		this.registerModule(dubboProperties.getModule(), beanFactory);
		this.registerProvider(dubboProperties.getProvider(), beanFactory);
		this.registerConsumer(dubboProperties.getConsumer(), beanFactory);
		this.registerReferences(dubboProperties.getReferences(), beanFactory);
		this.registerServices(dubboProperties.getServices(), beanFactory);
		super.postProcessBeanFactory(beanFactory);
	}

	private void registerConsumer(ConsumerConfig consumer, ConfigurableListableBeanFactory beanFactory) {
		if (consumer != null)
			beanFactory.registerSingleton("consumerConfig", consumer);
		else
			logger.debug("dubbo 没有配置默认的消费者参数");
	}

	@SuppressWarnings("deprecation")
	private void registerProvider(ProviderConfig provider, ConfigurableListableBeanFactory beanFactory) {
		if (provider != null) {
			beanFactory.registerSingleton("providerConfig", provider);
			if (provider.getPort() == null || provider.getPort() == 0) {
				provider.setPort(SocketUtils.findAvailableTcpPort(50000, 60000));
			}
		} else
			logger.debug("dubbo 没有配置默认的生成者参数");
	}

	private void registerModule(ModuleConfig module, ConfigurableListableBeanFactory beanFactory) {
		if (module != null)
			beanFactory.registerSingleton("moduleConfig", module);
		else
			logger.debug("dubbo 没有配置模块信息");
	}

	private void registerReferences(List<ReferenceConfig<?>> references, ConfigurableListableBeanFactory beanFactory) {
		if (references == null || references.isEmpty()) {
			return;
		}
		for (ReferenceConfig<?> referenceConfig : references) {
			String beanName = referenceConfig.getId() + "-ReferenceConfig";
			beanFactory.registerSingleton(beanName, referenceConfig);
			beanFactory.registerSingleton(referenceConfig.getId(), referenceConfig.get());
			logger.debug("注册调用信息{} 完毕", beanName);
		}
	}

	private void registerServices(List<ServiceConfig<?>> services, ConfigurableListableBeanFactory beanFactory) {
		if (services == null || services.isEmpty()) {
			return;
		}
		for (ServiceConfig<?> serviceConfig : services) {
			String beanName = serviceConfig.getId() + "-ServiceConfig";
			beanFactory.registerSingleton(beanName, serviceConfig);
			serviceConfig.export();
			logger.debug("注册服务信息{} 完毕", beanName);
		}
	}

	private void registerMonitor(MonitorConfig monitorConfig, ConfigurableListableBeanFactory beanFactory) {
		if (monitorConfig != null)
			beanFactory.registerSingleton("monitorConfig", monitorConfig);
		else
			logger.debug("dubbo 没有配置服务监控中心");
	}

	private void registerRegistry(RegistryConfig registryConfig, ConfigurableListableBeanFactory beanFactory) {
		if (registryConfig != null) {
			beanFactory.registerSingleton("registryConfig", registryConfig);
		} else
			logger.info("dubbo 没有配置服务注册中心");
	}

	private void registerThis(String annotationPackages, ConfigurableListableBeanFactory beanFactory) {
		if (StringUtils.isEmpty(annotationPackages)) {
			annotationPackages = ANNOTATION_PACKAGE;
			// logger.warn("dubbo没有配置注解服务所在的目录");
		}
		logger.info("dubbo扫描组件目录:{}", annotationPackages);
		this.setPackage(annotationPackages);
		super.setId("dubboAnnotationPackageS");
	}

	private void registerApplication(ApplicationConfig applicationConfig, ConfigurableListableBeanFactory beanFactory) {
		String applicationName = environment.resolvePlaceholders(NAME_PATTERN);
		logger.info("Start dubbo application name : " + applicationName);
		if (applicationConfig != null) {
			String name = applicationConfig.getName();
			if (StringUtils.isEmpty(name)) {
				name = applicationName;
				applicationConfig.setName(name);
			}
			beanFactory.registerSingleton(name, applicationConfig);
		} else {
			logger.warn("dubbo 没有配置服务名信息");
		}
	}

	private void registerProtocol(ProtocolConfig protocol, ConfigurableListableBeanFactory beanFactory) {
		if (protocol == null) {
			logger.warn("dubbo 没有配置协议,将使用默认协议");
			return;
		}
		String beanName = StringUtils.isEmpty(protocol.getName()) ? UUID.randomUUID().toString() : protocol.getName();
		if (protocol.getPort() == null || protocol.getPort() == 0) {
			protocol.setPort(SocketUtils.findAvailableTcpPort(50000, 60000));
		}
		beanFactory.registerSingleton(beanName, protocol);
		logger.debug("注册协议信息{}-ProtocolConfig 完毕", beanName);
	}

	private void registerProtocols(List<ProtocolConfig> protocols, ConfigurableListableBeanFactory beanFactory) {
		if (protocols == null || protocols.isEmpty()) {
			logger.debug("dubbo 没有配置协议,将使用默认协议");
			return;
		}
		for (ProtocolConfig protocol : protocols) {
			this.registerProtocol(protocol, beanFactory);
		}
	}

}

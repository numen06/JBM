package td.framework.boot.autoconfigure.druid;

import javax.sql.DataSource;

import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.alibaba.druid.support.spring.stat.BeanTypeAutoProxyCreator;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import com.td.util.StringUtils;

@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnClass({ DruidDataSource.class, DataSource.class })
@ConditionalOnProperty(prefix = "druid", name = "filters")
@EnableConfigurationProperties({ DataSourceProperties.class })
public class DruidAutoConfiguration {
	private final static Logger logger = LoggerFactory.getLogger(DruidAutoConfiguration.class);

	@Autowired
	private DataSourceProperties dataSourceProperties;
	@Autowired
	private ConfigurableEnvironment environment;

	@ConditionalOnProperty(prefix = "spring", name = "datasource.url")
	@Bean(name = "druidDataSource")
	@Primary
	public DruidDataSource dataSource(ConfigurableEnvironment environment) throws Exception {
		DruidDataSource druidDataSource = new DruidDataSource();
		DruidProperties druidProperties = new DruidProperties(environment, dataSourceProperties, druidDataSource);
		druidDataSource.setConnectProperties(druidProperties);
		druidProperties.afterPropertiesSet();
		logger.info("Start Druid Datasource URL : {}", druidDataSource.getUrl());
		return druidDataSource;
	}

	@Bean(name = "druid-stat-interceptor")
	@ConditionalOnMissingBean
	public DruidStatInterceptor druidStatInterceptor() {
		return new DruidStatInterceptor();
	}

	/**
	 * 网页监控设置
	 * 
	 * @author wesley
	 *
	 */
	@Configuration
	@ConditionalOnWebApplication
	@AutoConfigureAfter({ WebMvcAutoConfiguration.class })
	public class DruidWebMonitorConfig {

		@Bean
		public ServletRegistrationBean dispatcherRegistration() {
			String path = environment.resolvePlaceholders("${druid.monitor.path:druid}");
			String allow = environment.resolvePlaceholders("${druid.monitor.allow:}");
			String loginUsername = environment.resolvePlaceholders("${druid.monitor.loginUsername:druid}");
			String loginPassword = environment.resolvePlaceholders("${druid.monitor.loginPassword:druid}");
			ServletRegistrationBean registration = new ServletRegistrationBean(new StatViewServlet(), "/" + path + "/*");
			if (StringUtils.isNotBlank(allow))
				registration.addInitParameter("allow", allow);
			registration.addInitParameter("loginUsername", loginUsername);
			registration.addInitParameter("loginPassword", loginPassword);
			return registration;
		}

		@Bean
		public FilterRegistrationBean filterRegistrationBean() {
			String path = environment.resolvePlaceholders("${druid.monitor.path:druid}");
			FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
			filterRegistrationBean.setFilter(new WebStatFilter());
			filterRegistrationBean.setEnabled(true);
			filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/" + path + "/*");
			filterRegistrationBean.addUrlPatterns("/" + path + "/*");
			filterRegistrationBean.addInitParameter("sessionStatMaxCount", "1000");
			filterRegistrationBean.addInitParameter("sessionStatEnable", "true");
			// filterRegistrationBean.addInitParameter("principalSessionName",
			// "test");
			return filterRegistrationBean;
		}

		@Bean
		public BeanTypeAutoProxyCreator proxyCreator() throws ClassNotFoundException {
			BeanTypeAutoProxyCreator proxyCreator = new BeanTypeAutoProxyCreator();
			// proxyCreator.setProxyClassLoader(.getClassLoader());
			proxyCreator.setInterceptorNames("druid-stat-interceptor");
			Class<?> clazz;
			try {
				clazz = ClassUtils.getClass("com.td.framework.dao.mybatis.IBaseSqlDao");
			} catch (Exception e) {
				clazz = ClassUtils.getClass("com.td.framework.dao.IBaseSqlDao");
			}
			proxyCreator.setTargetBeanType(clazz);
			return proxyCreator;
		}
	}

}

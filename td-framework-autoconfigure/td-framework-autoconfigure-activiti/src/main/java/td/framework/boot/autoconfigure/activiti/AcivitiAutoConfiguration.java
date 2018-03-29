package td.framework.boot.autoconfigure.activiti;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.engine.impl.rules.RulesDeployer;
import org.activiti.spring.SpringAsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.ActivitiProperties;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ConditionalOnClass({ DataSource.class })
@ConditionalOnBean({ DataSource.class })
@AutoConfigureBefore(JpaProcessEngineAutoConfiguration.class)
@AutoConfigureAfter(name = { "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration", "td.framework.boot.autoconfigure.mybatis.MybatisAutoConfiguration" })
// @AutoConfigureBefore(name = { "MybatisAutoConfiguration",
// "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration" })
@EnableConfigurationProperties({ ActivitiProperties.class })
public class AcivitiAutoConfiguration extends AbstractProcessEngineAutoConfiguration {

	@Autowired
	protected ActivitiProperties activitiProperties;

	@Configuration
	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	@EnableConfigurationProperties(ActivitiProperties.class)
	public static class JpaConfiguration extends AbstractProcessEngineAutoConfiguration {
//		@Autowired(required = false)
//		private SqlSessionFactory sqlSessionFactory;
		@Autowired(required = false)
		private DataSource dataSource;

		@Bean
		@ConditionalOnMissingBean
		public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			return new JpaTransactionManager(emf);
		}

		@Bean
		@Primary
		public SpringProcessEngineConfiguration springProcessEngineConfiguration(EntityManagerFactory entityManagerFactory, PlatformTransactionManager transactionManager,
			SpringAsyncExecutor springAsyncExecutor) throws IOException {
			activitiProperties.setCheckProcessDefinitions(false);
			SpringProcessEngineConfiguration config = this.baseSpringProcessEngineConfiguration(dataSource, transactionManager, springAsyncExecutor);
			config.setJpaEntityManagerFactory(entityManagerFactory);
			config.setTransactionManager(transactionManager);
//			if (sqlSessionFactory != null)
//				config.setSqlSessionFactory(sqlSessionFactory);
			// config.setJpaHandleTransaction(activitiProperties.getJpaHandleTransaction());
			// config.setJpaCloseEntityManager(activitiProperties.getJpaCloseEntityManager());
			// config.setAsyncExecutorActivate(activitiProperties.getAsyncExecutorActivate());
			// config.setAsyncExecutorEnabled(activitiProperties.getAsyncExecutorEnabled());
			// config.setJobExecutorActivate(activitiProperties.getJobExecutorActivate());
			List<Deployer> customPostDeployers = new ArrayList<Deployer>();
			customPostDeployers.add(new RulesDeployer());
			config.setCustomPostDeployers(customPostDeployers);
			return config;
		}

	}

}

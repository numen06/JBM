package jbm.framework.boot.autoconfigure.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.uncode.schedule.ZKScheduleManager;
import cn.uncode.schedule.web.ManagerServlet;

@Configuration
@EnableConfigurationProperties({ ScheduleProperties.class })
public class ScheduleAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ScheduleAutoConfiguration.class);

	@Autowired
	private ScheduleProperties scheduleProperties;

	@Bean(name = "zkScheduleManager", initMethod = "init")
	public ZKScheduleManager commonMapper() {
		ZKScheduleManager zkScheduleManager = new ZKScheduleManager();
		zkScheduleManager.setZkConfig(scheduleProperties.getConfig());
		logger.info("=====>ZKScheduleManager inited..");
		return zkScheduleManager;
	}

	@Bean
	public ClusterScheduleManager clusterScheduleManager(ZKScheduleManager zKScheduleManager) {
		ClusterScheduleManager clusterScheduleManager = new ClusterScheduleManager(zKScheduleManager);
		return clusterScheduleManager;
	}

	// @ConditionalOnClass(SchedulerFactoryBean.class)
	// @ConditionalOnMissingBean(SchedulerFactoryBean.class)
	// @Bean(name = "quartzSchedulerFactoryBean")
	// public SchedulerFactoryBean quartzSchedulerFactoryBean() {
	// SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
	// if (scheduleProperties.getQuartzBean() != null &&
	// scheduleProperties.getQuartzMethod() != null &&
	// scheduleProperties.getQuartzCronExpression() != null) {
	// int len = scheduleProperties.getQuartzBean().size();
	// List<Trigger> triggers = new ArrayList<Trigger>();
	// for (int i = 0; i < len; i++) {
	// String name = scheduleProperties.getQuartzBean().get(i);
	// String method = scheduleProperties.getQuartzMethod().get(i);
	// String cronExpression =
	// scheduleProperties.getQuartzCronExpression().get(i);
	// if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(method) &&
	// StringUtils.isNotBlank(cronExpression)) {
	// MethodInvokingJobDetailFactoryBean methodInvokingJob = new
	// MethodInvokingJobDetailFactoryBean();
	// methodInvokingJob.setTargetBeanName(name);
	// methodInvokingJob.setTargetMethod(method);
	// CronTriggerFactoryBean cronTrigger = new CronTriggerFactoryBean();
	// cronTrigger.setJobDetail(methodInvokingJob.getObject());
	// triggers.add(cronTrigger.getObject());
	// }
	// }
	// if (triggers != null && triggers.size() > 0) {
	// schedulerFactoryBean.setTriggers((Trigger[]) triggers.toArray());
	// }
	// }
	// logger.info("=====>QuartzSchedulerFactoryBean inited..");
	// return schedulerFactoryBean;
	// }

	@Configuration
	@ConditionalOnWebApplication
	@AutoConfigureAfter({ WebMvcAutoConfiguration.class })
	public class DruidWebMonitorConfig {

		@Bean
		public ServletRegistrationBean dispatcherRegistration() {
			ServletRegistrationBean registration = new ServletRegistrationBean(new ManagerServlet(), scheduleProperties.getRootPath());
			return registration;
		}
	}

}

package jbm.framework.boot.autoconfigure.beetl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import jbm.framework.beetl.core.BeetlTemplate;
import jbm.framework.beetl.core.GroupTemplateFactaryBean;

@EnableConfigurationProperties(BeetlProperties.class)
public class BeetlAutoConfiguration {

	@Autowired
	private BeetlProperties beetlConfiguration;

	@Bean
	public BeetlTemplate beetlTemplate(GroupTemplateFactaryBean groupTemplateFactaryBean) {
		return new BeetlTemplate(groupTemplateFactaryBean);
	}

	@Bean
	public GroupTemplateFactaryBean groupTemplateFactaryBean() {
		GroupTemplateFactaryBean groupTemplateFactaryBean = new GroupTemplateFactaryBean();
		groupTemplateFactaryBean.setConfigFileResource(beetlConfiguration.getConfig());
		return groupTemplateFactaryBean;
	}
}

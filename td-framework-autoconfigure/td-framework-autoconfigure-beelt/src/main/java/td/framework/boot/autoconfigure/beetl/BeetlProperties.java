package td.framework.boot.autoconfigure.beetl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "beetl")
public class BeetlProperties {

	private Resource config = new ClassPathResource("td/framework/beetl/core/beetl-default.properties");

	public Resource getConfig() {
		return config;
	}

	public void setConfig(Resource config) {
		this.config = config;
	}

}

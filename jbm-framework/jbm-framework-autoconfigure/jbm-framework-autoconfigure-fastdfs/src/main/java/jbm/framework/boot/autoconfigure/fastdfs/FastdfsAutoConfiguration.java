package jbm.framework.boot.autoconfigure.fastdfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.fastdfs.FastdfsTemplate;

@Configuration
@EnableConfigurationProperties(FastdfsProperties.class)
@ConditionalOnProperty(prefix = "fastdfs", name = "trackerServers")
public class FastdfsAutoConfiguration {

	@Autowired
	private FastdfsProperties ftpProperties;

	@Bean
	public FastdfsTemplate fastdfsTemplate() throws Exception {
		return new FastdfsTemplate(ftpProperties.getTrackerServers());
	}

}

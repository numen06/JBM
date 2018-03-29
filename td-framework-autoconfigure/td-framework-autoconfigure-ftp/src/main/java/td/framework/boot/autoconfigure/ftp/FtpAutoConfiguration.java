package td.framework.boot.autoconfigure.ftp;

import java.io.IOException;
import java.net.SocketException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.ftp.FtpTemplate;

@Configuration
@EnableConfigurationProperties(FtpProperties.class)
@ConditionalOnProperty(prefix = "spring.data.ftp", name = "hostname")
public class FtpAutoConfiguration {

	@Autowired
	private FtpProperties ftpProperties;

	@Bean
	public FtpTemplate ftpTemplate() throws SocketException, IOException {
		return new FtpTemplate(ftpProperties);
	}

}

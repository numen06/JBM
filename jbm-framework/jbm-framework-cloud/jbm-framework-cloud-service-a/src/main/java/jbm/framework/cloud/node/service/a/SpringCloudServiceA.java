package jbm.framework.cloud.node.service.a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("jbm.framework.cloud")
public class SpringCloudServiceA {
	public static void main(String[] args) {
		SpringApplication.run(SpringCloudServiceA.class, args);
	}
}

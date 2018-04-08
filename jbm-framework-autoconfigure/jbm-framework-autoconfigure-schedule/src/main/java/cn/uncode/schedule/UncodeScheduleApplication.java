package cn.uncode.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by KevinBlandy on 2017/2/28 14:00
 */
@SpringBootApplication
@ComponentScan({"cn.uncode.schedule"})
@EnableScheduling
@ServletComponentScan
public class UncodeScheduleApplication {
	public static void main(String[] agrs){
		SpringApplication.run(UncodeScheduleApplication.class,agrs);
	}
}

package pres.lnk.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pres.lnk.springframework.annotation.EnableClusterScheduling;

import java.util.concurrent.TimeUnit;

/**
 * @Author lnk
 * @Date 2019/3/26
 */
@SpringBootApplication
@EnableClusterScheduling
public class Run {
    public static void main(String[] args) throws InterruptedException {
//        args = new String[]{"--server.port=8080"};
        SpringApplication.run(Run.class, args);
        TimeUnit.HOURS.sleep(1);
    }
}

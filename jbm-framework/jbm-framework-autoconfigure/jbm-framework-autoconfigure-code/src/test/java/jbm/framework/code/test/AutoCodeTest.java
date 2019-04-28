package jbm.framework.code.test;

import jbm.framework.code.EnableCodeAutoGeneate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

/**
 * @author: create by wesley
 * @date:2019/4/28
 */

@SpringBootApplication
@EnableCodeAutoGeneate(value = "jbm.framework.code.test.entity", targetPackage = "jbm.framework.code.test")
public class AutoCodeTest {

    public static void main(String[] args) throws InterruptedException {
//        args = new String[]{"--server.port=8080"};
        SpringApplication.run(AutoCodeTest.class, args);
//        TimeUnit.HOURS.sleep(1);
    }
}

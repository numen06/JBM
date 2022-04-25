package com.jbm.test;

import jbm.framework.boot.autoconfigure.swagger.annotation.EnableSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author wesley.zhang
 * @create 2021/7/14 7:38 下午
 * @email numen06@qq.com
 */
@EnableSwagger2
//@springfox.documentation.swagger2.annotations.EnableSwagger2
@SpringBootApplication
public class SwaggerApp {


    public static void main(String[] args) {
        SpringApplication.run(SwaggerApp.class, args);
    }

}

package com.jbm.dic.test;

import com.jbm.autoconfig.dic.annotation.EnableJbmDictionary;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJbmDictionary(basePackageClasses = PileDealStatusDict3.class)
public class DicApp {

    public static void main(String[] args) {
        SpringApplication.run(DicApp.class, args);
    }
}

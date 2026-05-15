package com.jbm.micro.mysql;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Liquibase + MyBatis-Plus + H2 联调入口（默认 {@code h2} profile）。JPA 不参与业务；若需一次性库表初始化请单独使用 {@code jbm-framework-autoconfigure-jpa} 工具模块或离线脚本。
 */
@SpringBootApplication
@MapperScan("com.jbm.micro.mysql.mapper")
public class MicroMysqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroMysqlApplication.class, args);
    }
}

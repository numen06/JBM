package com.jbm.examples.extendfield.business;

import jbm.framework.boot.autoconfigure.extendfield.FieldDefinitionSource;
import jbm.framework.boot.autoconfigure.extendfield.annotation.EnableExtendField;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 业务侧：字段定义只读 Redis，请求体按 formCode 拆分，值写入 extend_data。
 * <p>默认 {@code server.port=18082}，需配置侧先发布表单到同一 Redis。</p>
 */
@SpringBootApplication
@MapperScan("com.jbm.examples.extendfield.business.mapper")
@EnableExtendField(source = FieldDefinitionSource.REDIS)
public class ExtendFieldBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExtendFieldBusinessApplication.class, args);
    }
}

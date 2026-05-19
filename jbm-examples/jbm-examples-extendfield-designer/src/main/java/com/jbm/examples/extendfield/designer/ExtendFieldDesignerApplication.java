package com.jbm.examples.extendfield.designer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 配置/设计器侧：表单定义入库为真源，变更后发布到 Redis。
 * <p>默认 {@code server.port=18081}，与业务示例共用同一 Redis。</p>
 */
@SpringBootApplication
@MapperScan("com.jbm.examples.extendfield.designer.mapper")
public class ExtendFieldDesignerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExtendFieldDesignerApplication.class, args);
    }
}

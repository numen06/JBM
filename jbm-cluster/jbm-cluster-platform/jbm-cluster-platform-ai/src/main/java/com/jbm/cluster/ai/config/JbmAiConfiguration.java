package com.jbm.cluster.ai.config;

import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JBM AI 配置
 * @author wesley
 */
@Configuration
public class JbmAiConfiguration {

    /**
     * 配置 JbmRequestTemplate Bean
     * 用于执行服务间调用
     */
    @Bean
    public JbmRequestTemplate jbmRequestTemplate() {
        return new JbmRequestTemplate();
    }
}


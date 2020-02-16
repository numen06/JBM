package jbm.framework.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.servlet.http.HttpServletRequest;

/**
 * @program: jbm
 * @author: wesley.zhang
 * @create: 2020-02-16 23:43
 **/
@Slf4j
@ConditionalOnClass(HttpServletRequest.class)
public class WebConfiguration {

    /**
     * 全局异常处理配置
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(GlobalDefaultExceptionHandler.class)
    public GlobalDefaultExceptionHandler exceptionHandler() {
        GlobalDefaultExceptionHandler exceptionHandler = new GlobalDefaultExceptionHandler();
        log.info("OpenGlobalExceptionHandler [{}]", exceptionHandler);
        return exceptionHandler;
    }
}

package jbm.framework.boot.autoconfigure.extendfield;

import jbm.framework.boot.autoconfigure.extendfield.advice.ExtendFieldRequestBodyAdvice;
import jbm.framework.boot.autoconfigure.extendfield.advice.ResultExtendAop;
import jbm.framework.boot.autoconfigure.extendfield.service.LocalFieldDefinitionFallbackService;
import jbm.framework.boot.autoconfigure.extendfield.service.LocalFieldDefinitionService;
import jbm.framework.boot.autoconfigure.extendfield.web.ExtendFieldDefinitionController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 扩展字段自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ExtendFieldProperties.class)
@Import({
        LocalFieldDefinitionService.class,
        LocalFieldDefinitionFallbackService.class, // REDIS 且无 RedisService 时生效
        ExtendFieldRequestBodyAdvice.class,
        ResultExtendAop.class,
        ExtendFieldDefinitionController.class,
        ExtendFieldRedisImportSelector.class
})
public class ExtendFieldAutoConfiguration {
}

package jbm.framework.boot.autoconfigure.extendfield.bootstrap;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.FieldDefinitionSource;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 启动时将本地 YAML 中的字段定义同步到 Redis。
 */
@Slf4j
@Component
@ConditionalOnBean(FieldDefinitionAdminService.class)
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "sync-local-to-redis-on-startup", havingValue = "true", matchIfMissing = true)
public class ExtendFieldLocalBootstrap {

    @Resource
    private ExtendFieldProperties properties;

    @Resource
    private FieldDefinitionAdminService fieldDefinitionAdminService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getSource() == FieldDefinitionSource.LOCAL) {
            return;
        }
        Map<String, ExtendFieldProperties.FormDefinition> definitions = properties.getDefinitions();
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ExtendFieldProperties.FormDefinition> entry : definitions.entrySet()) {
            List<FieldDefinition> fields = entry.getValue().getFields();
            if (fields != null && !fields.isEmpty()) {
                fieldDefinitionAdminService.saveFieldDefinitions(entry.getKey(), fields);
                log.debug("Synced local extend field definitions to Redis: formCode={}", entry.getKey());
            }
        }
    }
}

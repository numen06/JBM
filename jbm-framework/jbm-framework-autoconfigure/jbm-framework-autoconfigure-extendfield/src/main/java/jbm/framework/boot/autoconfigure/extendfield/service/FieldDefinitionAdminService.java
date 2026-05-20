package jbm.framework.boot.autoconfigure.extendfield.service;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldScope;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扩展字段定义写入 Redis（元数据管理）。
 */
@Slf4j
@Service
@ConditionalOnClass(name = "jbm.framework.boot.autoconfigure.redis.RedisService")
public class FieldDefinitionAdminService implements FieldDefinitionWriter {

    @Resource
    private RedisService redisService;

    @Resource
    private ExtendFieldProperties properties;

    public void saveFieldDefinitions(String formCode, List<FieldDefinition> definitions) {
        if (formCode == null || formCode.isEmpty()) {
            return;
        }
        String scoped = ExtendFieldScope.scopedFormCode(properties, formCode);
        String formKey = RedisFieldDefinitionService.KEY_PREFIX_FORM + scoped;
        String namesKey = RedisFieldDefinitionService.KEY_PREFIX_NAMES + scoped;

        redisService.deleteObject(formKey);
        redisService.deleteObject(namesKey);

        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        Map<String, String> fieldMap = new HashMap<>();
        Set<String> names = new HashSet<>();
        for (Object raw : definitions) {
            FieldDefinition def = LocalFieldDefinitionService.toFieldDefinition(raw);
            if (def == null || def.getFieldName() == null || def.getFieldName().isEmpty()) {
                continue;
            }
            fieldMap.put(def.getFieldName(), JSON.toJSONString(def));
            names.add(def.getFieldName());
        }
        redisService.setCacheMap(formKey, fieldMap);
        redisService.setCacheSet(namesKey, names);
        log.info("Saved {} extend field definitions for scope={}", names.size(), scoped);
    }
}

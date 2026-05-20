package jbm.framework.boot.autoconfigure.extendfield.service;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import jbm.framework.boot.autoconfigure.extendfield.tenant.ExtendFieldScope;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 Redis 读取扩展字段定义。
 */
@Slf4j
@Service
@ConditionalOnClass(name = "jbm.framework.boot.autoconfigure.redis.RedisService")
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "source", havingValue = "REDIS", matchIfMissing = true)
public class RedisFieldDefinitionService implements FieldDefinitionService {

    static final String KEY_PREFIX_FORM = "extend_field:form:";
    static final String KEY_PREFIX_NAMES = "extend_field:names:";

    @Resource
    private RedisService redisService;

    @Resource
    private ExtendFieldProperties properties;

    @Override
    public Set<String> getExtendFieldNames(String formCode) {
        if (formCode == null || formCode.isEmpty()) {
            return Collections.emptySet();
        }
        String scoped = ExtendFieldScope.scopedFormCode(properties, formCode);
        String namesKey = KEY_PREFIX_NAMES + scoped;
        Set<Object> raw = redisService.getCacheSet(namesKey);
        if (raw != null && !raw.isEmpty()) {
            Set<String> names = new HashSet<>();
            for (Object o : raw) {
                if (o != null) {
                    names.add(o.toString());
                }
            }
            return names;
        }
        return getFieldDefinitionMap(formCode).keySet();
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions(String formCode) {
        return new ArrayList<>(getFieldDefinitionMap(formCode).values());
    }

    @Override
    public FieldDefinition getFieldDefinition(String formCode, String fieldName) {
        return getFieldDefinitionMap(formCode).get(fieldName);
    }

    @Override
    public void refreshCache(String formCode) {
        // Redis 为真源，刷新由 Admin 重写；此处保留接口供扩展本地二级缓存
        log.debug("extend field cache refresh requested for formCode={}", formCode);
    }

    Map<String, FieldDefinition> getFieldDefinitionMap(String formCode) {
        String scoped = ExtendFieldScope.scopedFormCode(properties, formCode);
        String formKey = KEY_PREFIX_FORM + scoped;
        Map<String, Object> rawMap = redisService.getCacheMap(formKey);
        if (rawMap == null || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, FieldDefinition> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            FieldDefinition def = parseDefinition(entry.getValue());
            if (def != null) {
                if (def.getFieldName() == null) {
                    def.setFieldName(entry.getKey());
                }
                result.put(entry.getKey(), def);
            }
        }
        return result;
    }

    private static FieldDefinition parseDefinition(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof FieldDefinition) {
            return (FieldDefinition) value;
        }
        if (value instanceof String) {
            return JSON.parseObject((String) value, FieldDefinition.class);
        }
        return JSON.parseObject(JSON.toJSONString(value), FieldDefinition.class);
    }
}

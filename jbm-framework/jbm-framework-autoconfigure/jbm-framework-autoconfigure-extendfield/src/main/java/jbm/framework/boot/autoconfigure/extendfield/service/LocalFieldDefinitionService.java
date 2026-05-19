package jbm.framework.boot.autoconfigure.extendfield.service;

import com.alibaba.fastjson.JSON;
import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 从本地 YAML 读取扩展字段定义（不依赖 Redis）。
 */
@Service
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "source", havingValue = "LOCAL")
public class LocalFieldDefinitionService implements FieldDefinitionService {

    @Resource
    private ExtendFieldProperties properties;

    @Override
    public Set<String> getExtendFieldNames(String formCode) {
        List<FieldDefinition> list = getFieldDefinitions(formCode);
        if (list.isEmpty()) {
            return Collections.emptySet();
        }
        return list.stream()
                .map(FieldDefinition::getFieldName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions(String formCode) {
        ExtendFieldProperties.FormDefinition form = properties.getDefinitions().get(formCode);
        if (form == null || form.getFields() == null) {
            return Collections.emptyList();
        }
        List<FieldDefinition> resolved = new ArrayList<>();
        for (Object raw : form.getFields()) {
            FieldDefinition def = toFieldDefinition(raw);
            if (def != null) {
                resolved.add(def);
            }
        }
        return resolved;
    }

    /**
     * YAML / 配置绑定后列表元素可能是 Map、JSONObject，而非 {@link FieldDefinition}。
     */
    public static FieldDefinition toFieldDefinition(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof FieldDefinition) {
            return (FieldDefinition) raw;
        }
        return JSON.parseObject(JSON.toJSONString(raw), FieldDefinition.class);
    }

    @Override
    public FieldDefinition getFieldDefinition(String formCode, String fieldName) {
        return getFieldDefinitionMap(formCode).get(fieldName);
    }

    @Override
    public void refreshCache(String formCode) {
        // 本地配置无运行时缓存
    }

    private Map<String, FieldDefinition> getFieldDefinitionMap(String formCode) {
        Map<String, FieldDefinition> map = new LinkedHashMap<>();
        for (FieldDefinition def : getFieldDefinitions(formCode)) {
            if (def != null && def.getFieldName() != null) {
                map.put(def.getFieldName(), def);
            }
        }
        return map;
    }
}

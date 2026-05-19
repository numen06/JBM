package jbm.framework.boot.autoconfigure.extendfield.service;

import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;

import java.util.List;
import java.util.Set;

/**
 * 扩展字段定义查询（元数据，存 Redis / 本地配置）。
 */
public interface FieldDefinitionService {

    Set<String> getExtendFieldNames(String formCode);

    List<FieldDefinition> getFieldDefinitions(String formCode);

    FieldDefinition getFieldDefinition(String formCode, String fieldName);

    void refreshCache(String formCode);
}

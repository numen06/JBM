package jbm.framework.boot.autoconfigure.extendfield.service;

import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;

import java.util.List;

/**
 * 扩展字段定义持久化（Redis 等实现）。
 */
public interface FieldDefinitionWriter {

    void saveFieldDefinitions(String formCode, List<FieldDefinition> definitions);
}

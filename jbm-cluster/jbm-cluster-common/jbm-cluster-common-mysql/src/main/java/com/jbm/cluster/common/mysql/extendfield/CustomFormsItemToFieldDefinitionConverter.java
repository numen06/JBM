package com.jbm.cluster.common.mysql.extendfield;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.center.FieldType;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomFormsItem（UI 设计器）→ 运行时 FieldDefinition（Redis）。
 */
public final class CustomFormsItemToFieldDefinitionConverter {

    private CustomFormsItemToFieldDefinitionConverter() {
    }

    public static List<FieldDefinition> convert(List<CustomFormsItem> items) {
        List<FieldDefinition> result = new ArrayList<>();
        if (CollUtil.isEmpty(items)) {
            return result;
        }
        for (CustomFormsItem item : items) {
            if (item == null || StrUtil.isBlank(item.getFieldName())) {
                continue;
            }
            FieldDefinition def = new FieldDefinition();
            def.setFieldName(item.getFieldName());
            def.setFieldLabel(StrUtil.blankToDefault(item.getLabelName(), item.getFieldName()));
            def.setFieldType(mapFieldType(item.getFieldType()));
            def.setRequired(Boolean.TRUE.equals(item.getIsRequired()));
            def.setQueryable(Boolean.TRUE.equals(item.getIsFilter()));
            result.add(def);
        }
        return result;
    }

    private static String mapFieldType(FieldType fieldType) {
        if (fieldType == null) {
            return "string";
        }
        if (fieldType == FieldType.number) {
            return "number";
        }
        if (fieldType == FieldType.date) {
            return "date";
        }
        return "string";
    }
}

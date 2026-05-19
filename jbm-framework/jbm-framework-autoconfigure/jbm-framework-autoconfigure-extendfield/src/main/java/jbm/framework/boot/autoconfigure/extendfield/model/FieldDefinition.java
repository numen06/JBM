package jbm.framework.boot.autoconfigure.extendfield.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 扩展字段元数据定义。
 */
@Data
public class FieldDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldName;
    private String fieldType;
    private String fieldLabel;
    private Boolean required;
    private Boolean sortable;
    private Boolean queryable;
    private Object defaultValue;
    private Map<String, Object> options;
}

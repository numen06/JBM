package com.jbm.micro.mysql.mp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 动态表单定义（库表为真源）。
 */
@Data
@TableName(value = "md_extend_form_definition", autoResultMap = true)
public class MdExtendFormDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("form_code")
    private String formCode;

    private String formName;

    @TableField(value = "fields_json", typeHandler = JacksonTypeHandler.class)
    private List<FieldDefinition> fields;

    private Integer version;

    private Date updateTime;
}

package com.jbm.cluster.api.entitys.center;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jbm.framework.boot.autoconfigure.extendfield.model.FieldDefinition;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 扩展字段表单定义（库表真源，发布后写入 Redis）。
 */
@Data
@Entity
@Table
@TableName(value = "extend_form_definition", autoResultMap = true)
@ApiModel("扩展字段表单定义")
public class ExtendFormDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @TableField("tenant_id")
    @ApiModelProperty("租户/应用 ID")
    private Long tenantId;

    @TableField("form_code")
    @ApiModelProperty("表单编码")
    private String formCode;

    @ApiModelProperty("表单名称")
    private String formName;

    @TableField(value = "fields_json", typeHandler = JacksonTypeHandler.class)
    @ApiModelProperty("字段定义 JSON")
    private List<FieldDefinition> fields;

    @ApiModelProperty("版本号")
    private Integer version;

    @TableField("custom_form_id")
    @ApiModelProperty("关联 custom_forms.id")
    private Long customFormId;

    @ApiModelProperty("更新时间")
    private Date updateTime;
}

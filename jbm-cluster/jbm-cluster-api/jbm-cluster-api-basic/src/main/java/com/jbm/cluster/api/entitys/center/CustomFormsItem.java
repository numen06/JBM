package com.jbm.cluster.api.entitys.center;


import com.jbm.cluster.api.constants.center.ComponentType;
import com.jbm.cluster.api.constants.center.FieldType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 11:39
 */
@Data
@Entity
@Table
@TableName(value = "custom_forms_item", autoResultMap = true)
@ApiModel("自定义表单字段")
public class CustomFormsItem extends MasterDataEntity {

    @ApiModelProperty("所属表单id")
    @NotEmpty(message = "所属表单id不能为空")
    private Long formId;
    @ApiModelProperty("字段名称")
    @NotEmpty(message = "字段名称不能为空")
    private String fieldName;
    @ApiModelProperty("标签名称")
    @NotEmpty(message = "标签名称不能为空")
    private String labelName;
    @ApiModelProperty("字段/数据类型-字典field_type")
    @NotEmpty(message = "数据类型不能为空")
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;
    @ApiModelProperty("组件类型-字典component_type")
    @NotEmpty(message = "数据类型不能为空")
    @Enumerated(EnumType.STRING)
    private ComponentType componentType;
    @ApiModelProperty("格式")
    private String format;
    //数值子选项
    @ApiModelProperty("数值子选项 0 保留小数位 1 显示千分符")
    private String decimalType;
    @ApiModelProperty("数值子选项值（保留小数位数）")
    private Integer decimalValue;
    //单选或多选的子选项
    @ApiModelProperty("单选或多选的子选项 0 数据源 1 数据字典")
    private String choiceType;
    @ApiModelProperty("单选或多选的子选项的值")
    private String choiceValue;
    //日期的子选项
    @ApiModelProperty("日期的子选项 0 默认系统日期")
    private String dateType;

    @ApiModelProperty("是否必填")
    private Boolean isRequired;
    @ApiModelProperty("是否显示")
    private Boolean isShow;
    @ApiModelProperty("是否为筛选项")
    private Boolean isFilter;
    @ApiModelProperty("字段所属 0 系统字段 1 自定义字段")
    private String fieldBelong;
    @ApiModelProperty("valueKey")
    private String valueKey;
    @ApiModelProperty("labelKey")
    private String labelKey;
    @ApiModelProperty("childrenKey")
    private String childrenKey;
}

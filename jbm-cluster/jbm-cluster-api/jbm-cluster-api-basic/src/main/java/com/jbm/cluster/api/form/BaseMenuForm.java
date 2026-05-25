package com.jbm.cluster.api.form;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("菜单资源表单")
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseMenuForm extends BaseMenu {
    @ApiModelProperty("分页参数")
    @TableField(exist = false)
    private PageForm pageForm;

    @ApiModelProperty("菜单范围: platform | app | visible | all")
    @TableField(exist = false)
    private String scope;

    @ApiModelProperty("关键字(编码/名称/路径)")
    @TableField(exist = false)
    private String keyword;
}
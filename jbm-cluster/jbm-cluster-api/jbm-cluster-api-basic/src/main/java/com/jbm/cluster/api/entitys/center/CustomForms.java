package com.jbm.cluster.api.entitys.center;

import com.jbm.cluster.api.constants.center.FormOrTable;
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
import javax.validation.constraints.NotNull;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 10:13
 */
@Data
@Entity
@Table
@TableName(value = "custom_forms", autoResultMap = true)
@ApiModel("自定义表单")
public class CustomForms extends MasterDataEntity {

    @ApiModelProperty("名称")
    @NotEmpty(message = "名称不能为空")
    private String name;
    @ApiModelProperty("所属菜单id")
    private String menuIds;
    @ApiModelProperty("类型 字典form_table")
    @NotNull(message = "类型不能为空")
    @Enumerated(EnumType.STRING)
    private FormOrTable formOrTable;
    @ApiModelProperty("数据源")
    private String dataSource;
}

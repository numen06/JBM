package com.jbm.cluster.api.entitys.center;

import com.jbm.cluster.api.constants.center.FormOrTable;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 10:13
 */
@Data
@Entity
@Table
@ApiModel("自定义表单")
public class CustomForms extends MasterDataIdEntity {
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

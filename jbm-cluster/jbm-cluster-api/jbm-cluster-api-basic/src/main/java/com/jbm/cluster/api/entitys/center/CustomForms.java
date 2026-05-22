package com.jbm.cluster.api.entitys.center;

import com.jbm.cluster.api.constants.center.FormOrTable;
import com.baomidou.mybatisplus.annotation.TableField;
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
import java.util.Map;

/**
 * @author scolin
 * @description
 * @date 2025/7/23 10:13
 */
@Data
@Entity
@Table
@ApiModel("自定义表单")
public class CustomForms extends MasterDataEntity {

    /** 旧库表可能无下列 MasterData 列，查询时排除 */
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private Long appId;
    @TableField(exist = false)
    private Long parentId;
    @TableField(exist = false)
    private Integer level;
    @TableField(exist = false)
    private String leafPath;
    @TableField(exist = false)
    private Map<String, Object> extendData;

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

package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_user_config")
@ApiModel(value = "用户配置管理")
public class BaseUserConfig extends MasterDataIdEntity {

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "角色ID")
    private String roleId;

    @ApiModelProperty(value = "应用ID")
    private String appId;

    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "配置内容")
    private String configContent;
}

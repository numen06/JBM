package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.cluster.api.entitys.auth.AuthorityExt;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * API Key 与权限关联
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("API Key权限")
@TableName("base_authority_apikey")
public class BaseAuthorityApikey extends AuthorityExt {

    @ApiModelProperty("API Key ID")
    private Long keyId;

    @ApiModelProperty("授权状态: 1-启用 2-禁用")
    private Integer authStatus;
}

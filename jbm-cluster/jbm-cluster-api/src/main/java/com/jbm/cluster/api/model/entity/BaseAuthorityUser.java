package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.cluster.api.model.AuthorityExt;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 系统权限-用户关联
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("用户权限设置")
@TableName("base_authority_user")
public class BaseAuthorityUser extends AuthorityExt {

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

}

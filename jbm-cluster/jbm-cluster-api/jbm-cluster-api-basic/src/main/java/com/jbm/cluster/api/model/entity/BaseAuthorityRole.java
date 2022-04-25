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
 * 系统权限-角色关联
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("角色权限设置")
@TableName("base_authority_role")
public class BaseAuthorityRole extends AuthorityExt {

    /**
     * 角色ID
     */
    @ApiModelProperty("角色ID")
    private Long roleId;


}

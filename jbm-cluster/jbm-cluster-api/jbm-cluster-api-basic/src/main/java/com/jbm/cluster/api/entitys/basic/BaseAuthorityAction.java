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
 * 系统权限-功能操作关联表
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("按钮权限设置")
@TableName("base_authority_action")
public class BaseAuthorityAction extends AuthorityExt {
    private static final long serialVersionUID = 1471599074044557390L;
    /**
     * 操作资源ID
     */
    @ApiModelProperty("按钮ID")
    private Long actionId;

}

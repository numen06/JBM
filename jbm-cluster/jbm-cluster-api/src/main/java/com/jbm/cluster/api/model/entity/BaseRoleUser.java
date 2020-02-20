package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 系统角色-角色与用户关联
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_role_user")
public class BaseRoleUser extends MasterDataEntity {
    /**
     * 系统用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

}

package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统角色-角色与用户关联
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_role_user")
public class BaseRoleUser extends MasterDataEntity {
    private static final long serialVersionUID = -667816444278087761L;
    /**
     * 系统用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取角色ID
     *
     * @return role_id - 角色ID
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * 设置角色ID
     *
     * @param roleId 角色ID
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}

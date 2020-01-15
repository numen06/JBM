package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.AuthorityMenu;
import com.jbm.cluster.api.model.entity.BaseAuthorityRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jbm.cluster.common.security.OpenAuthority;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseAuthorityRoleMapper extends BaseMapper<BaseAuthorityRole> {

    /**
     * 获取角色已授权权限
     *
     * @param roleId
     * @return
     */
    List<OpenAuthority> selectAuthorityByRole(@Param("roleId") Long roleId);

    /**
     * 获取角色菜单权限
     *
     * @param roleId
     * @return
     */
    List<AuthorityMenu> selectAuthorityMenuByRole(@Param("roleId") Long roleId);
}

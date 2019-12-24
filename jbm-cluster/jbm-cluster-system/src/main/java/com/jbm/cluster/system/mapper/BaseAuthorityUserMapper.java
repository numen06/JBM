package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.AuthorityMenu;
import com.jbm.cluster.api.model.entity.BaseAuthorityUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jbm.cluster.common.security.OpenAuthority;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liuyadu
 */
@Repository
public interface BaseAuthorityUserMapper extends BaseMapper<BaseAuthorityUser> {

    /**
     * 获取用户已授权权限
     *
     * @param userId
     * @return
     */
    List<OpenAuthority> selectAuthorityByUser(@Param("userId") Long userId);

    /**
     * 获取用户已授权权限完整信息
     *
     * @param userId
     * @return
     */
    List<AuthorityMenu> selectAuthorityMenuByUser(@Param("userId") Long userId);
}

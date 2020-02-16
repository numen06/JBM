package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.entity.BaseRole;
import com.jbm.cluster.api.model.entity.BaseRoleUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseRoleUserMapper extends BaseMapper<BaseRoleUser> {
    /**
     * 查询系统用户角色
     *
     * @param userId
     * @return
     */
    List<BaseRole> selectRoleUserList(@Param("userId") Long userId);

    /**
     * 查询用户角色ID列表
     * @param userId
     * @return
     */
    List<Long> selectRoleUserIdList(@Param("userId") Long userId);
}

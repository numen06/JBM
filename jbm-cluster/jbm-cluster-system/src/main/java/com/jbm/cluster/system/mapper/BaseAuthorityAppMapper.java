package com.jbm.cluster.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jbm.cluster.api.model.entity.BaseAuthorityApp;
import com.jbm.cluster.common.security.OpenAuthority;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseAuthorityAppMapper extends BaseMapper<BaseAuthorityApp> {

    /**
     * 获取应用已授权权限
     *
     * @param appId
     * @return
     */
    List<OpenAuthority> selectAuthorityByApp(@Param("appId") String appId);
}

package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.basic.BaseAuthorityApp;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseAuthorityAppMapper extends SuperMapper<BaseAuthorityApp> {

    /**
     * 获取应用已授权权限
     *
     * @param appId
     * @return
     */
    List<OpenAuthority> selectAuthorityByApp(@Param("appId") Long appId);
}

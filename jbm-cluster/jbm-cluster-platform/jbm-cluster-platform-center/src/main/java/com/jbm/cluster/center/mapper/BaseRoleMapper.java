package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.entity.BaseRole;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseRoleMapper extends SuperMapper<BaseRole> {

    List<BaseRole> selectRoleList(Map params);
}

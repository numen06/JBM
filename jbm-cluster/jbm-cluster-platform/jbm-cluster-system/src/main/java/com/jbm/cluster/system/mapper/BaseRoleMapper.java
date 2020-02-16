package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.entity.BaseRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseRoleMapper extends BaseMapper<BaseRole> {

    List<BaseRole> selectRoleList(Map params);
}

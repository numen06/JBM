package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.AuthorityAction;
import com.jbm.cluster.api.model.AuthorityApi;
import com.jbm.cluster.api.model.AuthorityMenu;
import com.jbm.cluster.api.model.AuthorityResource;
import com.jbm.cluster.api.model.entity.BaseAuthority;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jbm.cluster.common.security.OpenAuthority;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author liuyadu
 */
@Repository
public interface BaseAuthorityMapper extends BaseMapper<BaseAuthority> {

    /**
     * 查询所有资源授权列表
     * @return
     */
    List<AuthorityResource> selectAllAuthorityResource();

    /**
     * 查询已授权权限列表
     *
     * @param map
     * @return
     */
    List<OpenAuthority> selectAuthorityAll(Map map);


    /**
     * 获取菜单权限
     *
     * @param map
     * @return
     */
    List<AuthorityMenu> selectAuthorityMenu(Map map);

    /**
     * 获取操作权限
     *
     * @param map
     * @return
     */
    List<AuthorityAction> selectAuthorityAction(Map map);

    /**
     * 获取API权限
     *
     * @param map
     * @return
     */
    List<AuthorityApi> selectAuthorityApi(Map map);


}

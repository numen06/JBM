package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.auth.AuthorityAction;
import com.jbm.cluster.api.entitys.auth.AuthorityApi;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.entitys.basic.BaseAuthority;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Delete;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseAuthorityMapper extends SuperMapper<BaseAuthority> {

    /**
     * 查询所有资源授权列表
     *
     * @return
     */
    List<AuthorityResource> selectAllAuthorityResource();

    @Delete("DELETE FROM base_account WHERE  user_id NOT IN (SELECT user_id FROM base_user)")
    void clearAccount();

    @Delete("DELETE FROM base_account_logs WHERE  user_id NOT IN (SELECT user_id FROM base_user)")
    void clearAccountLogs();


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

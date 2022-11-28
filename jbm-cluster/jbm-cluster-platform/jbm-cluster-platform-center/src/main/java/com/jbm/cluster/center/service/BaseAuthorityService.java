package com.jbm.cluster.center.service;

import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.auth.AuthorityApi;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.auth.AuthorityResource;
import com.jbm.cluster.api.entitys.basic.BaseAuthority;
import com.jbm.cluster.api.entitys.basic.BaseAuthorityAction;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 系统权限管理
 *
 * @author wesley.zhang
 */
public interface BaseAuthorityService extends IMasterDataService<BaseAuthority> {

    /**
     * 获取访问权限列表
     *
     * @return
     */
    List<AuthorityResource> findAuthorityResource();

    List<AuthorityMenu> findAuthorityMenu(Integer status);

    /**
     * 获取菜单权限列表
     *
     * @param status
     * @return
     */
    List<AuthorityMenu> findAuthorityMenu(Integer status, Long appId);


    /**
     * 获取API权限列表
     *
     * @param serviceId
     * @return
     */
    List<AuthorityApi> findAuthorityApi(String serviceId);


    /**
     * 查询功能按钮权限列表
     *
     * @param actionId
     * @return
     */
    List<BaseAuthorityAction> findAuthorityAction(Long actionId);


    /**
     * 保存或修改权限
     *
     * @param resourceId
     * @param resourceType
     * @return 权限Id
     */
    BaseAuthority saveOrUpdateAuthority(Long resourceId, ResourceType resourceType);


    /**
     * 获取权限
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    BaseAuthority getAuthority(Long resourceId, ResourceType resourceType);

    /**
     * 移除权限
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    void removeAuthority(Long resourceId, ResourceType resourceType);

    /**
     * 移除应用权限
     *
     * @param appId
     */
    void removeAuthorityApp(Long appId);


    /**
     * 移除功能按钮权限
     *
     * @param actionId
     */
    void removeAuthorityAction(Long actionId);

    /**
     * 是否已被授权
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    Boolean isGranted(Long resourceId, ResourceType resourceType);

    /**
     * 角色授权
     *
     * @param
     * @param roleId       角色ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return 权限标识
     */
    void addAuthorityRole(Long roleId, Date expireTime, String... authorityIds);


    /**
     * 用户授权
     *
     * @param
     * @param userId       用户ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return 权限标识
     */
    void addAuthorityUser(Long userId, Date expireTime, String... authorityIds);


    /**
     * 应用授权
     *
     * @param
     * @param appId        应用ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return
     */
    void addAuthorityApp(Long appId, Date expireTime, String... authorityIds);

    /**
     * 应用授权-添加单个权限
     *
     * @param appId
     * @param expireTime
     * @param authorityId
     */
    void addAuthorityApp(Long appId, Date expireTime, String authorityId);

    /**
     * 添加功能按钮权限
     *
     * @param actionId
     * @param authorityIds
     * @return
     */
    void addAuthorityAction(Long actionId, String... authorityIds);

    /**
     * 获取应用已授权权限
     *
     * @param appId
     * @return
     */
    List<OpenAuthority> findAuthorityByApp(Long appId);

    /**
     * 获取角色已授权权限
     *
     * @param roleId
     * @return
     */
    List<OpenAuthority> findAuthorityByRole(Long roleId);

    /**
     * 获取用户已授权权限
     *
     * @param userId
     * @param root
     * @return
     */
    List<OpenAuthority> findAuthorityByUser(Long userId, Boolean root);

    /**
     * 获取开放对象权限
     *
     * @param type = null 查询全部  type = 1 获取菜单和操作 type = 2 获取API
     * @return
     */
    List<OpenAuthority> findAuthorityByType(String type);

    /**
     * 获取用户已授权权限详情
     *
     * @param userId
     * @param root
     * @return
     */
    List<AuthorityMenu> findAuthorityMenuByUser(Long userId, Long appId, Boolean root);

    /**
     * 检测全是是否被多个角色授权
     *
     * @param authorityId
     * @param roleIds
     * @return
     */
    Boolean isGrantedByRoleIds(String authorityId, Long... roleIds);


    /**
     * 清理无效权限
     *
     * @param serviceId
     * @param codes
     */
    void clearInvalidApi(String serviceId, Collection<String> codes);
}

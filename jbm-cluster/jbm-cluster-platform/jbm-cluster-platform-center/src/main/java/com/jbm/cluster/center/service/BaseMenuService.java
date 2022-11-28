package com.jbm.cluster.center.service;

import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * 菜单资源管理
 *
 * @author wesley.zhang
 */
public interface BaseMenuService extends IMasterDataService<BaseMenu> {

    DataPaging<BaseMenu> findListPage(PageRequestBody pageRequestBody);

    List<BaseMenu> findPlatformList(BaseMenu baseMenu);

    /**
     * 查询列表
     *
     * @return
     */
    List<BaseMenu> findAllList(BaseMenu baseMenu);

    /**
     * 根据主键获取菜单
     *
     * @param menuId
     * @return
     */
    BaseMenu getMenu(Long menuId);


    /**
     * 根据主键获取菜单
     *
     * @param appId
     * @return
     */
    List<BaseMenu> getMenuByAppId(Long appId);


    /**
     * 检查菜单编码是否存在
     *
     * @param menuCode
     * @return
     */
    Boolean isExist(String menuCode);


    /**
     * 添加菜单资源
     *
     * @param menu
     * @return
     */
    BaseMenu addMenu(BaseMenu menu);

    /**
     * 修改菜单资源
     *
     * @param menu
     * @return
     */
    BaseMenu updateMenu(BaseMenu menu);

    /**
     * 移除菜单
     *
     * @param menuId
     * @return
     */
    void removeMenu(Long menuId);
}

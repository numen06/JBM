package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * 菜单资源管理
 *
 * @author wesley.zhang
 */
public interface BaseMenuService extends IMasterDataService<BaseMenu> {

    DataPaging<BaseMenu> findListPage(BaseMenuForm form);

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
     * 检查菜单编码是否存在（同一应用或平台公共范围内唯一）
     */
    Boolean isExist(String menuCode, Long appId);


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

    /**
     * 根据path和appId查询菜单
     *
     * @param path
     * @param appId
     * @return
     */
    BaseMenu getMenuByPathAndAppId(String path, Long appId);

    /**
     * 根据 menuCode 与 appId 查询菜单
     */
    BaseMenu getMenuByCode(String menuCode, Long appId);

    /**
     * 批量导入菜单
     */
    int importMenus(List<BaseMenu> menus);

    /**
     * 从源应用同步菜单到目标应用（含按钮）
     *
     * @param sourceAppId 模板应用，通常为 JBM
     * @param targetAppId 目标应用
     * @param mode        merge 增量同步；replace 先清空目标非保留菜单再全量复制
     */
    int syncMenusFromApp(Long sourceAppId, Long targetAppId, String mode);
}

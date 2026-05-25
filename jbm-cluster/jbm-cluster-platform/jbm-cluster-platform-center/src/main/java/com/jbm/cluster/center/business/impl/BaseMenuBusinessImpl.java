package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.cluster.center.business.BaseMenuBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.BaseMenuService;
import com.jbm.cluster.common.mysql.service.MenuDataScopeHelper;
import com.jbm.framework.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class BaseMenuBusinessImpl implements BaseMenuBusiness {

    @Autowired
    private BaseMenuService baseMenuService;
    @Autowired
    private MenuDataScopeHelper menuDataScopeHelper;
    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public BaseMenu addMenuWithGatewayRefresh(BaseMenuForm form) {
        BaseMenu menu = BeanUtil.toBean(form, BaseMenu.class);
        menuDataScopeHelper.assertCanManageAppId(menu.getAppId());
        BaseMenu result = baseMenuService.addMenu(menu);
        jbmClusterTemplate.refreshGateway();
        return result;
    }

    @Override
    public void updateMenuWithGatewayRefresh(BaseMenuForm form) {
        BaseMenu existing = baseMenuService.getMenu(form.getMenuId());
        BaseMenu menu = BeanUtil.toBean(form, BaseMenu.class);
        menuDataScopeHelper.assertCanModifyExistingMenu(existing, menu);
        baseMenuService.updateMenu(menu);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void removeMenuWithGatewayRefresh(Long menuId) {
        BaseMenu existing = baseMenuService.getMenu(menuId);
        menuDataScopeHelper.assertCanManageMenu(existing);
        baseMenuService.removeMenu(menuId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public List<BaseMenu> listMenusForExport(Long appId) {
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(appId);
        List<BaseMenu> list;
        if (ObjectUtil.isEmpty(appId)) {
            list = baseMenuService.findPlatformList(baseMenu);
        } else {
            list = baseMenuService.findAllList(baseMenu);
        }
        List<BaseMenu> exportList = new ArrayList<>();
        for (BaseMenu menu : list) {
            BaseMenu exportMenu = new BaseMenu();
            exportMenu.setMenuId(menu.getMenuId());
            exportMenu.setMenuCode(menu.getMenuCode());
            exportMenu.setMenuName(menu.getMenuName());
            exportMenu.setIcon(menu.getIcon());
            exportMenu.setParentId(menu.getParentId());
            exportMenu.setScheme(menu.getScheme());
            exportMenu.setPath(menu.getPath());
            exportMenu.setTarget(menu.getTarget());
            exportMenu.setPriority(menu.getPriority());
            exportMenu.setMenuDesc(menu.getMenuDesc());
            exportMenu.setStatus(menu.getStatus());
            exportMenu.setIsPersist(menu.getIsPersist());
            exportMenu.setServiceId(menu.getServiceId());
            exportMenu.setAppId(menu.getAppId());
            exportMenu.setHidden(menu.getHidden());
            exportList.add(exportMenu);
        }
        return exportList;
    }

    @Override
    public int importMenusWithGatewayRefresh(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ServiceException("import file required");
            }
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (ObjectUtil.isEmpty(jsonContent)) {
                throw new ServiceException("import file is empty");
            }
            List<BaseMenu> menus = JSON.parseArray(jsonContent, BaseMenu.class);
            if (menus == null || menus.isEmpty()) {
                throw new ServiceException("import file format error");
            }
            int successCount = baseMenuService.importMenus(menus);
            jbmClusterTemplate.refreshGateway();
            return successCount;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.of(e, "import menu failed: " + e.getMessage());
        }
    }
}

package com.jbm.cluster.center.business;

import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.cluster.common.mysql.service.BaseMenuService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BaseMenuBusiness extends BaseMenuService {

    BaseMenu addMenuWithGatewayRefresh(BaseMenuForm form);

    void updateMenuWithGatewayRefresh(BaseMenuForm form);

    void removeMenuWithGatewayRefresh(Long menuId);

    List<BaseMenu> listMenusForExport(Long appId);

    int importMenusWithGatewayRefresh(MultipartFile file);
}
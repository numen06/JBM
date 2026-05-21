package com.jbm.cluster.center.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.center.business.BaseMenuBusiness;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import com.jbm.cluster.common.mysql.service.BaseMenuService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.BaseController;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 系统菜单资源管理
 */
@Api(tags = "系统菜单资源管理")
@RestController
@RequestMapping("/menu")
public class BaseMenuController extends BaseController {

    @Autowired
    private BaseMenuBusiness baseMenuBusiness;
    @Autowired
    private BaseMenuService baseMenuService;
    @Autowired
    private BaseActionService baseActionService;

    @ApiOperation(value = "菜单分页列表")
    @GetMapping
    public ResultBody<DataPaging<BaseMenu>> listMenus(@ModelAttribute BaseMenuForm form) {
        return ResultBody.callback(() -> baseMenuService.findListPage(form != null ? form : new BaseMenuForm()));
    }

    @ApiOperation(value = "菜单列表")
    @GetMapping("/all")
    public ResultBody<List<BaseMenu>> listAllMenus(@RequestParam(required = false) Long appId) {
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(appId);
        if (ObjectUtil.isEmpty(appId)) {
            return ResultBody.callback(() -> baseMenuService.findPlatformList(baseMenu));
        }
        return ResultBody.callback(() -> baseMenuService.findAllList(baseMenu));
    }

    @ApiOperation(value = "当前登录应用菜单")
    @GetMapping("/current")
    public ResultBody<List<BaseMenu>> listCurrentMenus() {
        JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser();
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(jbmLoginUser.getAppId());
        return ResultBody.callback(() -> baseMenuService.findAllList(baseMenu));
    }

    @ApiOperation(value = "导出菜单")
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportMenus(@RequestParam(required = false) Long appId, HttpServletResponse response) throws IOException {
        List<BaseMenu> exportList = baseMenuBusiness.listMenusForExport(appId);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        WebUtils.setFileDownloadHeader(response, "menus.json");
        response.getOutputStream().write(JSON.toJSONBytes(exportList));
        response.getOutputStream().flush();
    }

    @ApiOperation(value = "导入菜单")
    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultBody<String> importMenus(@RequestParam("file") MultipartFile file) {
        return ResultBody.callback(() -> {
            int count = baseMenuBusiness.importMenusWithGatewayRefresh(file);
            return String.format("成功导入 %d 个菜单", count);
        });
    }

    @ApiOperation(value = "菜单详情")
    @GetMapping("/{menuId}")
    public ResultBody<BaseMenu> getMenu(@PathVariable Long menuId) {
        return ResultBody.callback(() -> baseMenuService.getMenu(menuId));
    }

    @ApiOperation(value = "菜单操作")
    @GetMapping("/{menuId}/actions")
    public ResultBody<List<BaseAction>> listMenuActions(@PathVariable Long menuId) {
        return ResultBody.callback(() -> baseActionService.findListByMenuId(menuId));
    }

    @ApiOperation(value = "创建菜单")
    @PostMapping
    public ResultBody<Long> createMenu(@RequestBody BaseMenuForm form) {
        return ResultBody.callback(() -> {
            BaseMenu result = baseMenuBusiness.addMenuWithGatewayRefresh(form);
            return result != null ? result.getMenuId() : null;
        });
    }

    @ApiOperation(value = "更新菜单")
    @PutMapping("/{menuId}")
    public ResultBody<Void> updateMenu(@PathVariable Long menuId, @RequestBody BaseMenuForm form) {
        form.setMenuId(menuId);
        baseMenuBusiness.updateMenuWithGatewayRefresh(form);
        return ResultBody.ok();
    }

    @ApiOperation(value = "删除菜单")
    @DeleteMapping("/{menuId}")
    public ResultBody<Void> deleteMenu(@PathVariable Long menuId) {
        baseMenuBusiness.removeMenuWithGatewayRefresh(menuId);
        return ResultBody.ok();
    }
}

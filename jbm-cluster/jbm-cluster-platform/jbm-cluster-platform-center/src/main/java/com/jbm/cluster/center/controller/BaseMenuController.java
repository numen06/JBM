package com.jbm.cluster.center.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.center.service.BaseActionService;
import com.jbm.cluster.center.service.BaseMenuService;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import jbm.framework.web.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Api(tags = "系统菜单资源管理")
@RestController
@RequestMapping("/menu")
public class BaseMenuController extends MasterDataCollection<BaseMenu, BaseMenuService> {
    @Autowired
    private BaseMenuService baseResourceMenuService;

    @Autowired
    private BaseActionService baseResourceOperationService;

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    /**
     * 获取分页菜单资源列表
     *
     * @return
     */
    @ApiOperation(value = "获取分页菜单资源列表", notes = "获取分页菜单资源列表")
    @GetMapping("/")
    public ResultBody<DataPaging<BaseMenu>> getMenuListPage(@RequestParam(required = false) Map map) {
        return ResultBody.callback(() -> baseResourceMenuService.findListPage(PageRequestBody.from(map)));
    }

    /**
     * 菜单所有资源列表
     *
     * @return
     */
    @ApiOperation(value = "菜单所有资源列表", notes = "菜单所有资源列表")
    @GetMapping("/all")
    public ResultBody<List<BaseMenu>> getMenuAllList(@RequestParam(required = false) Long appId) {
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(appId);
        if (ObjectUtil.isEmpty(appId)) {
            return ResultBody.callback("查询平台菜单成功", () -> {
                return baseResourceMenuService.findPlatformList(baseMenu);
            });
        }
        return ResultBody.callback("查询平台菜单成功", () -> {
            return baseResourceMenuService.findAllList(baseMenu);
        });
    }

    @ApiOperation(value = "导出菜单JSON文件")
    @GetMapping("/exportMenu")
    public void exportMenu(@RequestParam(required = false) Long appId, HttpServletResponse response) throws IOException {
        String fileName = "menus.json";
        List<BaseMenu> list = new ArrayList<>();
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(appId);
        if (ObjectUtil.isEmpty(appId)) {
            list = baseResourceMenuService.findPlatformList(baseMenu);
        } else {
            list = baseResourceMenuService.findAllList(baseMenu);
        }
        
        // 清理不需要导出的字段，保留业务字段
        List<BaseMenu> exportList = new ArrayList<>();
        for (BaseMenu menu : list) {
            BaseMenu exportMenu = new BaseMenu();
            exportMenu.setMenuId(menu.getMenuId()); // 保留ID用于parentId映射
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
            // 不导出createTime和updateTime，这些会在导入时自动生成
            exportList.add(exportMenu);
        }
        
        //将list写入response作为JSON导出
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        WebUtils.setFileDownloadHeader(response, fileName);
        byte[] jsonBytes = JSON.toJSONBytes(exportList);

        // 写入响应输出流
        response.getOutputStream().write(jsonBytes);
        response.getOutputStream().flush();
    }

    @ApiOperation(value = "导入菜单JSON文件")
    @PostMapping("/importMenu")
    public ResultBody<String> importMenu(@RequestParam(value = "file", required = false) MultipartFile file) {
        return ResultBody.callback(() -> {
            try {
                if (file == null || file.isEmpty()) {
                    throw new ServiceException("请选择要导入的文件");
                }
                
                // 读取文件内容
                String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                
                if (ObjectUtil.isEmpty(jsonContent)) {
                    throw new ServiceException("导入文件内容为空");
                }
                
                // 解析JSON为菜单列表
                List<BaseMenu> menus = JSON.parseArray(jsonContent, BaseMenu.class);
                
                if (menus == null || menus.isEmpty()) {
                    throw new ServiceException("导入文件内容为空或格式错误");
                }
                
                // 批量导入菜单
                int successCount = baseResourceMenuService.importMenus(menus);
                
                // 刷新网关
                jbmClusterTemplate.refreshGateway();
                
                return String.format("成功导入 %d 个菜单", successCount);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                throw ServiceException.of(e, "导入菜单失败: " + e.getMessage());
            }
        });
    }




    @ApiOperation(value = "获取当前系统所有菜单", notes = "获取当前系统所有菜单")
    @GetMapping("/currentAllMenu")
    public ResultBody<List<BaseMenu>> currentAllMenu() {
        JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser();
        BaseMenu baseMenu = new BaseMenu();
        baseMenu.setAppId(jbmLoginUser.getAppId());
        return ResultBody.callback(() -> baseResourceMenuService.findAllList(baseMenu));
    }


    /**
     * 获取菜单下所有操作
     *
     * @param menuId
     * @return
     */
    @ApiOperation(value = "获取菜单下所有操作", notes = "获取菜单下所有操作")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "menuId", paramType = "form"),
    })
    @GetMapping("/action")
    public ResultBody<List<BaseAction>> getMenuAction(Long menuId) {
        return ResultBody.callback(() -> baseResourceOperationService.findListByMenuId(menuId));
    }

    /**
     * 获取菜单资源详情
     *
     * @param menuId
     * @return 应用信息
     */
    @ApiOperation(value = "获取菜单资源详情", notes = "获取菜单资源详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", required = true, value = "menuId"),
    })
    @GetMapping("/{menuId}/info")
    public ResultBody<BaseMenu> getMenu(@PathVariable("menuId") Long menuId) {
        return ResultBody.callback(() -> baseResourceMenuService.getMenu(menuId));
    }

    /**
     * 添加菜单资源
     *
     * @param menuCode 菜单编码
     * @param menuName 菜单名称
     * @param icon     图标
     * @param scheme   请求前缀
     * @param path     请求路径
     * @param target   打开方式
     * @param status   是否启用
     * @param parentId 父节点ID
     * @param priority 优先级越小越靠前
     * @param menuDesc 描述
     * @return
     */
    @ApiOperation(value = "添加菜单资源", notes = "添加菜单资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuCode", required = true, value = "菜单编码", paramType = "form"),
            @ApiImplicitParam(name = "menuName", required = true, value = "菜单名称", paramType = "form"),
            @ApiImplicitParam(name = "icon", required = false, value = "图标", paramType = "form"),
            @ApiImplicitParam(name = "scheme", required = false, value = "请求协议", allowableValues = "/,http://,https://", paramType = "form"),
            @ApiImplicitParam(name = "path", required = false, value = "请求路径", paramType = "form"),
            @ApiImplicitParam(name = "target", required = false, value = "请求路径", allowableValues = "_self,_blank", paramType = "form"),
            @ApiImplicitParam(name = "parentId", required = false, defaultValue = "0", value = "父节点ID", paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "priority", required = false, value = "优先级越小越靠前", paramType = "form"),
            @ApiImplicitParam(name = "menuDesc", required = false, value = "描述", paramType = "form"),
            @ApiImplicitParam(name = "hidden", required = false, defaultValue = "1", value = "是否显示", paramType = "form"),

    })
    @PostMapping("/add")
    public ResultBody<Long> addMenu(
            @RequestParam(value = "menuCode") String menuCode,
            @RequestParam(value = "menuName") String menuName,
            @RequestParam(value = "icon", required = false) String icon,
            @RequestParam(value = "scheme", required = false, defaultValue = "/") String scheme,
            @RequestParam(value = "path", required = false, defaultValue = "") String path,
            @RequestParam(value = "target", required = false, defaultValue = "_self") String target,
            @RequestParam(value = "status", defaultValue = "1") Integer status,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "priority", required = false, defaultValue = "0") Integer priority,
            @RequestParam(value = "menuDesc", required = false, defaultValue = "") String menuDesc,
            @RequestParam(value = "hidden", required = false, defaultValue = "1") Integer hidden
    ) {
        return ResultBody.callback(() -> {
            BaseMenu menu = new BaseMenu();
            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setIcon(icon);
            menu.setPath(path);
            menu.setScheme(scheme);
            menu.setTarget(target);
            menu.setStatus(status);
            menu.setParentId(parentId);
            menu.setPriority(priority);
            menu.setMenuDesc(menuDesc);
            menu.setHidden(hidden);
            Long menuId = null;
            BaseMenu result = baseResourceMenuService.addMenu(menu);
            if (result != null) {
                menuId = result.getMenuId();
            }
            return menuId;
        });
    }


    /**
     * 编辑菜单资源
     *
     * @param menuCode 菜单编码
     * @param menuName 菜单名称
     * @param icon     图标
     * @param scheme   请求前缀
     * @param path     请求路径
     * @param target   打开方式
     * @param status   是否启用
     * @param parentId 父节点ID
     * @param priority 优先级越小越靠前
     * @param menuDesc 描述
     * @return
     */
    @ApiOperation(value = "编辑菜单资源", notes = "编辑菜单资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", required = true, value = "菜单ID", paramType = "form"),
            @ApiImplicitParam(name = "menuCode", required = true, value = "菜单编码", paramType = "form"),
            @ApiImplicitParam(name = "menuName", required = true, value = "菜单名称", paramType = "form"),
            @ApiImplicitParam(name = "icon", required = false, value = "图标", paramType = "form"),
            @ApiImplicitParam(name = "scheme", required = false, value = "请求协议", allowableValues = "/,http://,https://", paramType = "form"),
            @ApiImplicitParam(name = "path", required = false, value = "请求路径", paramType = "form"),
            @ApiImplicitParam(name = "target", required = false, value = "请求路径", allowableValues = "_self,_blank", paramType = "form"),
            @ApiImplicitParam(name = "parentId", required = false, defaultValue = "0", value = "父节点ID", paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "priority", required = false, value = "优先级越小越靠前", paramType = "form"),
            @ApiImplicitParam(name = "menuDesc", required = false, value = "描述", paramType = "form"),
            @ApiImplicitParam(name = "hidden", required = false, defaultValue = "1", value = "是否显示", paramType = "form"),
    })
    @PostMapping("/update")
    public ResultBody updateMenu(
            @RequestParam("menuId") Long menuId,
            @RequestParam(value = "menuCode") String menuCode,
            @RequestParam(value = "menuName") String menuName,
            @RequestParam(value = "icon", required = false) String icon,
            @RequestParam(value = "scheme", required = false, defaultValue = "/") String scheme,
            @RequestParam(value = "path", required = false, defaultValue = "") String path,
            @RequestParam(value = "target", required = false, defaultValue = "_self") String target,
            @RequestParam(value = "status", defaultValue = "1") Integer status,
            @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId,
            @RequestParam(value = "priority", required = false, defaultValue = "0") Integer priority,
            @RequestParam(value = "menuDesc", required = false, defaultValue = "") String menuDesc,
            @RequestParam(value = "hidden", required = false, defaultValue = "1") Integer hidden
    ) {
        BaseMenu menu = new BaseMenu();
        menu.setMenuId(menuId);
        menu.setMenuCode(menuCode);
        menu.setMenuName(menuName);
        menu.setIcon(icon);
        menu.setPath(path);
        menu.setScheme(scheme);
        menu.setTarget(target);
        menu.setStatus(status);
        menu.setParentId(parentId);
        menu.setPriority(priority);
        menu.setMenuDesc(menuDesc);
        menu.setHidden(hidden);
        baseResourceMenuService.updateMenu(menu);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }


//    @ApiOperation(value = "添加和编辑菜单资源", notes = "编辑菜单资源")
//    @PostMapping("/save")
//    public ResultBody<Long> updateMenu(@RequestBody BaseMenu menu) {
//        BaseMenu
//                result = baseResourceMenuService.saveEntity(menu);
//        openRestTemplate.refreshGateway();
//        return ResultBody.callback(() -> result);
//    }
//
//    /**
//     * 移除菜单资源
//     *
//     * @return
//     */
//    @ApiOperation(value = "移除菜单资源", notes = "移除菜单资源")
//    @PostMapping("/removeMenu")
//    public ResultBody<Boolean> removeMenu(@RequestBody BaseMenu menu) {
//        baseResourceMenuService.removeMenu(menu.getMenuId());
//        openRestTemplate.refreshGateway();
//        return ResultBody.ok();
//    }

    /**
     * 移除菜单资源
     *
     * @param menuId
     * @return
     */
    @ApiOperation(value = "移除菜单资源JSON", notes = "移除菜单资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", required = true, value = "menuId", paramType = "form"),
    })
    @PostMapping("/remove")
    public ResultBody<Boolean> removeMenu(
            @RequestParam("menuId") Long menuId
    ) {
        baseResourceMenuService.removeMenu(menuId);
        jbmClusterTemplate.refreshGateway();
        return ResultBody.ok();
    }
}

package com.jbm.cluster.center.controller;

import com.jbm.cluster.common.basic.service.SysDebugModeService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统调试模式管理（超级管理员）
 */
@Slf4j
@Api(tags = "系统调试模式")
@RestController
@RequestMapping("/system/debug")
public class SystemDebugController {

    @Autowired
    private SysDebugModeService sysDebugModeService;

    @ApiOperation("查询系统调试模式状态")
    @GetMapping
    public ResultBody<Map<String, Boolean>> getDebugMode() {
        requireAdmin();
        return ResultBody.ok(buildResponse(sysDebugModeService.isDebugModeEnabled()));
    }

    @ApiOperation("设置系统调试模式")
    @PostMapping
    public ResultBody<Map<String, Boolean>> setDebugMode(@RequestParam boolean enabled) {
        requireAdmin();
        boolean previous = sysDebugModeService.isDebugModeEnabled();
        sysDebugModeService.setDebugModeEnabled(enabled);
        log.info("[系统调试模式] 操作人={}, 变更: {} -> {}", LoginHelper.getUsername(), previous, enabled);
        return ResultBody.ok(buildResponse(enabled));
    }

    private void requireAdmin() {
        if (!LoginHelper.isAdmin()) {
            throw new ServiceException("无权限操作，仅超级管理员可管理系统调试模式");
        }
    }

    private Map<String, Boolean> buildResponse(boolean enabled) {
        Map<String, Boolean> body = new LinkedHashMap<>(1);
        body.put("enabled", enabled);
        return body;
    }
}

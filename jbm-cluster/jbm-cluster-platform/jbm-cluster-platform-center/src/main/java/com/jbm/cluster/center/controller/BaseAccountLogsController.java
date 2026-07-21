package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.center.service.BaseAccountLogsService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.mvc.web.AuditReadOnlyCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2021-09-16 15:35:49
 */
@Api(tags = "登陆日志")
@RestController
@RequestMapping("/baseAccountLogs")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class BaseAccountLogsController extends AuditReadOnlyCollection<BaseAccountLogs, BaseAccountLogsService> {
}

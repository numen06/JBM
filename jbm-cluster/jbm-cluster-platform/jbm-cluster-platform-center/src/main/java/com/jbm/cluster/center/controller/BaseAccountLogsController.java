package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.center.service.BaseAccountLogsService;
import com.jbm.framework.mvc.web.MasterDataCollection;
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
public class BaseAccountLogsController extends MasterDataCollection<BaseAccountLogs, BaseAccountLogsService> {
}

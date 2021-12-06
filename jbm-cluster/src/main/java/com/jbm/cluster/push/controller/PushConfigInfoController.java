package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.entitys.message.PushConfigInfo;
import com.jbm.cluster.push.service.PushConfigInfoService;
import com.jbm.framework.service.mybatis.MultiPlatformCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-11-19 16:21:00
 */
@Api(tags = "PushConfigInfo开放接口")
@RestController
@RequestMapping("/pushConfigInfo")
public class PushConfigInfoController extends MultiPlatformCollection<PushConfigInfo, PushConfigInfoService> {
}

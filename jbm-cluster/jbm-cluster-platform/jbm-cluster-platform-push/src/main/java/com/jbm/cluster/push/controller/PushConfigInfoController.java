package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.entity.message.PushConfigInfo;
import com.jbm.cluster.push.service.PushConfigInfoService;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-08-25 12:58:58
 */
@Api(tags = "推送配置设置")
@RestController
@RequestMapping("/pushConfigInfo")
public class PushConfigInfoController extends MultiPlatformCollection<PushConfigInfo, PushConfigInfoService> {

}

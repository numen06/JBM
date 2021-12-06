package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.model.entitys.message.PushItem;
import com.jbm.cluster.push.service.PushItemService;
import com.jbm.framework.service.mybatis.MultiPlatformTreeCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-11-19 16:21:00
 */
@Api(tags = "PushItem开放接口")
@RestController
@RequestMapping("/pushItem")
public class PushItemController extends MultiPlatformTreeCollection<PushItem, PushItemService> {
}

package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.PushItem;
import com.jbm.cluster.push.service.PushItemService;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2021-10-08 16:43:28
 */
@Api(tags = "PushItem开放接口")
@RestController
@RequestMapping("/pushItem")
public class PushItemController extends MultiPlatformCollection<PushItem, PushItemService> {
}

package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.message.PushItem;
import com.jbm.cluster.center.service.PushItemService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-08-25 12:54:02
 */
@Api(tags = "PushItem开放接口")
@RestController
@RequestMapping("/pushItem")
public class PushItemController extends MasterDataCollection<PushItem, PushItemService> {
}

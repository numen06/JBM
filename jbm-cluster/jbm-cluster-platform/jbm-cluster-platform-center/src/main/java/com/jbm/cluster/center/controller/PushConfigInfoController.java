package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.message.PushConfigInfo;
import com.jbm.cluster.center.service.PushConfigInfoService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-08-25 12:54:02
 */
@Api(tags = "PushConfigInfo开放接口")
@RestController
@RequestMapping("/pushConfigInfo")
public class PushConfigInfoController extends MasterDataCollection<PushConfigInfo, PushConfigInfoService> {
}

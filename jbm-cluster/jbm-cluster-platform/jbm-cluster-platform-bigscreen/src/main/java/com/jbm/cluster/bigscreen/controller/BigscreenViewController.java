package com.jbm.cluster.bigscreen.controller;

import com.jbm.cluster.api.model.entity.bigscreen.BigscreenView;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-09-03 17:08:07
 */
@Api(tags = "大屏管理接口")
@RestController
@RequestMapping("/bigscreenView")
public class BigscreenViewController extends MasterDataCollection<BigscreenView, BigscreenViewService> {
}

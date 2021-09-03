package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.BaseReleaseInfo;
import com.jbm.cluster.center.service.BaseReleaseInfoService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-08-25 10:49:30
 */
@Api(tags = "版本发布管理")
@RestController
@RequestMapping("/baseReleaseInfo")
public class BaseReleaseInfoController extends MasterDataCollection<BaseReleaseInfo, BaseReleaseInfoService> {
}

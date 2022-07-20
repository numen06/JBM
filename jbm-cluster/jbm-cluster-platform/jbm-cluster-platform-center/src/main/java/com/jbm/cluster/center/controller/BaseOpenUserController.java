package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseOpenUser;
import com.jbm.cluster.center.service.BaseOpenUserService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-19 14:01:27
 */
@Api(tags = "开放用户信息开放接口")
@RestController
@RequestMapping("/baseOpenUser")
public class BaseOpenUserController extends MasterDataCollection<BaseOpenUser, BaseOpenUserService> {
}

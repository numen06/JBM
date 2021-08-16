package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.BaseAuthorityUser;
import com.jbm.cluster.center.service.BaseAuthorityUserService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:57:09
 */
@RestController
@RequestMapping("/baseAuthorityUser")
public class BaseAuthorityUserController extends MasterDataCollection<BaseAuthorityUser, BaseAuthorityUserService> {
}
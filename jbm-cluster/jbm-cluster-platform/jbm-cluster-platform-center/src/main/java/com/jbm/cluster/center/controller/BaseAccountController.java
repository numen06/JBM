package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.center.service.BaseAccountService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-02-25 03:47:52
 */
@RestController
@RequestMapping("/baseAccount")
public class BaseAccountController extends MasterDataCollection<BaseAccount, BaseAccountService> {
}
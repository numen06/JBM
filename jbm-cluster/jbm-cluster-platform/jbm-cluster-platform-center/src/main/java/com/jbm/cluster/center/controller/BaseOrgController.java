package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.BaseOrg;
import com.jbm.cluster.center.service.BaseOrgService;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2020-03-24 03:28:09
 */
@Api(tags = "BaseOrg开放接口")
@RestController
@RequestMapping("/baseOrg")
public class BaseOrgController extends MasterDataTreeCollection<BaseOrg, BaseOrgService> {
}

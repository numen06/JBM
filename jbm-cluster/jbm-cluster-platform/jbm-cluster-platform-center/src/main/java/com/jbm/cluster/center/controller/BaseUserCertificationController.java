package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.cluster.center.service.BaseUserCertificationService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-07-19 14:01:27
 */
@Api(tags = "用户实名认证开放接口")
@RestController
@RequestMapping("/baseUserCertification")
public class BaseUserCertificationController extends MasterDataCollection<BaseUserCertification, BaseUserCertificationService> {
}

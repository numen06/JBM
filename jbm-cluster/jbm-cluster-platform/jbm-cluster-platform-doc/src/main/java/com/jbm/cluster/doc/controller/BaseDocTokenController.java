package com.jbm.cluster.doc.controller;

import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.cluster.doc.service.BaseDocTokenService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
@Api(tags = "文档分组管理开放接口")
@RestController
@RequestMapping("/baseDocToken")
public class BaseDocTokenController extends MasterDataCollection<BaseDocToken, BaseDocTokenService> {
}

package com.jbm.cluster.doc.controller;

import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-20 14:46:37
 */
@Api(tags = "文档管理开放接口")
@RestController
@RequestMapping("/baseDoc")
public class BaseDocController extends MasterDataCollection<BaseDoc, BaseDocService> {
}

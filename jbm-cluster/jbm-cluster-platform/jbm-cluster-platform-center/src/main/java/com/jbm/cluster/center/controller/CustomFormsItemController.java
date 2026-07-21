package com.jbm.cluster.center.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.center.CustomFormsItem;
import com.jbm.cluster.center.service.CustomFormsItemService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2025-07-23 16:11:49
 */
@Api(tags = "自定义表单字段开放接口")
@RestController
@RequestMapping("/customFormsItem")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class CustomFormsItemController extends MasterDataCollection<CustomFormsItem, CustomFormsItemService> {
}

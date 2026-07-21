package com.jbm.cluster.center.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.jbm.cluster.api.entitys.basic.Feedback;
import com.jbm.cluster.center.service.FeedbackService;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: wesley.zhang
 * @Create: 2022-03-15 12:13:48
 */
@Api(tags = "反馈管理开放接口")
@RestController
@RequestMapping("/feedback")
@SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
public class FeedbackController extends MasterDataCollection<Feedback, FeedbackService> {
}

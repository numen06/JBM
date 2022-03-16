package com.jbm.cluster.center.controller;

import com.jbm.cluster.api.model.entity.Feedback;
import com.jbm.cluster.center.service.FeedbackService;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-03-15 12:13:48
 */
@Api(tags = "反馈管理开放接口")
@RestController
@RequestMapping("/feedback")
public class FeedbackController extends MasterDataCollection<Feedback, FeedbackService> {
}

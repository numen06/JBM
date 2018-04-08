package com.jbm.framework.devops.actuator.web;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.github.pfmiles.org.apache.commons.lang.StringUtils;
import com.jbm.framework.devops.actuator.service.MonitorService;
import com.jbm.framework.form.JsonRequestBody;
import com.jbm.framework.metadata.bean.ResultForm;
import com.xiaoleilu.hutool.io.FileUtil;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

@RestController
@RequestMapping("/monitor")
public class SystemMonitorController {

	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(SystemMonitorController.class);

	@Autowired
	private MonitorService monitorService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@RequestMapping("/all")
	public Object monitorUsedCache() {
		try {
			return ResultForm.createSuccessResultForm(monitorService.getMonitorInfoBeanPools(), "查询成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "查询失败");
		}
	}

	@Scheduled(fixedRate = 2000)
	public void monitor() throws Exception {
		messagingTemplate.convertAndSend("/topic/monitor", JSON.toJSONString(monitorService.getMonitorInfoBean()));
	}

	@RequestMapping("/collect")
	public Object monitor(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			System.out.println(jsonRequestBody.toJSONString());
			return ResultForm.createSuccessResultForm(null, "监控注册成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "监控注册成功");
		}
	}

}

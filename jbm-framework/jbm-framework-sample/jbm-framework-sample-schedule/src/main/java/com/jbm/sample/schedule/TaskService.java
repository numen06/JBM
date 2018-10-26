package com.jbm.sample.schedule;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import cn.uncode.schedule.core.TaskDefine;
import jbm.framework.boot.autoconfigure.schedule.ClusterScheduleManager;

@Service
public class TaskService {
	private static int i = 0;

	private static int j = 0;

	@Autowired
	private ClusterScheduleManager clusterScheduleManager;

	private String uuid = UUID.randomUUID().toString();

	@Scheduled(fixedDelay = 3000)
	public void print() {
		System.out.println("===========注解方式start!=========");
		System.out.println(uuid + "I:" + i);
		i++;
		System.out.println(JSON.toJSONString(clusterScheduleManager.queryScheduleTask()));
		// System.err.println(JSON.toJSONString(clusterScheduleManager.queryScheduleTask()));
		System.out.println("=========== end !=========");
	}

	@PostConstruct
	public void init() {
		clusterScheduleManager.clearScheduleTask();
		TaskDefine define = clusterScheduleManager.createTaskFromCron(this, "print2", "0/5 * * * * ?", null, "我是天才");
		define.setExtKeySuffix("2");
		clusterScheduleManager.addScheduleTask(define);
		TaskDefine define2 = clusterScheduleManager.createTaskFromCron(this, "print2", "0/5 * * * * ?", null, "我是天才2");
		define.setExtKeySuffix("3");
		clusterScheduleManager.addScheduleTask(define2);
	}

	public void print2(String test) {
		System.out.println("===========动态方式start!=========");
		System.out.println(uuid + "J:" + j + test);
		j++;
		System.out.println(JSON.toJSONString(clusterScheduleManager.queryScheduleTask()));
		System.out.println("=========== end !=========");
	}

}

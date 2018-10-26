package com.jbm.framework.devops.actuator.service;

import java.util.List;

import com.jbm.framework.devops.actuator.bean.MonitorInfoBean;

public interface MonitorService {
	MonitorInfoBean getMonitorInfoBean() throws Exception;

	List<MonitorInfoBean> getMonitorInfoBeanPools();
}
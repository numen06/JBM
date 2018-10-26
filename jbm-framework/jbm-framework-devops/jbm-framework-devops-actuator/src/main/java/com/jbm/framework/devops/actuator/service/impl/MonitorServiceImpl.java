package com.jbm.framework.devops.actuator.service.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.jbm.framework.devops.actuator.bean.MonitorInfoBean;
import com.jbm.framework.devops.actuator.config.SigarService;
import com.jbm.framework.devops.actuator.service.MonitorService;

@Service
public class MonitorServiceImpl implements MonitorService {

	private static Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);

	private LinkedBlockingDeque<MonitorInfoBean> monitorInfoBeanPools = Queues.newLinkedBlockingDeque(300);

	@Autowired
	private SigarService sigarService;

	public MonitorInfoBean getMonitorInfoBean() throws Exception {
		if (this.monitorInfoBeanPools.size() <= 0)
			Thread.sleep(2000l);
		return this.monitorInfoBeanPools.getLast();
	}

	public Double systemCpuLoad() {
		try {
			CpuPerc cpuPerc = sigarService.getSigar().getCpuPerc();
			return cpuPerc.getCombined() * 100;
		} catch (SigarException e) {
			e.printStackTrace();
		}
		return 0.0;
	}

	public MonitorInfoBean loadMonitorInfoBean() throws Exception {
		int kb = 1024;
		Mem mem = sigarService.getSigar().getMem();
		double cpuRatio = systemCpuLoad();
		MonitorInfoBean infoBean = new MonitorInfoBean();
		infoBean.setMonitorTime(new Date());
		infoBean.setTotalMemory(mem.getTotal() / kb);
		infoBean.setUsedMemory(mem.getUsed() / kb);
		infoBean.setMemRatio(mem.getUsedPercent());
		infoBean.setCpuRatio(cpuRatio);
		return infoBean;
	}

	@Scheduled(fixedRate = 2000)
	public void systemLoad() {
		try {
			if (monitorInfoBeanPools.size() >= 300) {
				monitorInfoBeanPools.removeFirst();
			}
			monitorInfoBeanPools.add(this.loadMonitorInfoBean());
		} catch (Exception e) {
			logger.error("监测系统错误", e);
		}
	}

	@Override
	public List<MonitorInfoBean> getMonitorInfoBeanPools() {
		return Lists.newArrayList(monitorInfoBeanPools.iterator());
	}

}
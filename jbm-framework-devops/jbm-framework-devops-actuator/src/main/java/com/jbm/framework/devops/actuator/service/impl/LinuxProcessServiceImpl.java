/**
 * 
 */
package com.jbm.framework.devops.actuator.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbm.framework.devops.actuator.config.SigarService;
import com.jbm.framework.devops.actuator.service.ProcessService;

import jodd.io.FileNameUtil;

/**
 * @author Leonid
 *
 */
// @Service
public class LinuxProcessServiceImpl implements ProcessService {

	private final static Logger logger = LoggerFactory.getLogger(LinuxProcessServiceImpl.class);
	private SigarService sigarService;

	public LinuxProcessServiceImpl(SigarService sigarService) {
		super();
		this.sigarService = sigarService;
	}

	@Override
	public boolean exists(Integer pid) {
		try {
			if (pid == null)
				return false;
			if (sigarService != null)
				return sigarService.existsPid(pid);
			String[] cmds = { "/bin/sh", "-c", "ps aux|grep " + pid + "|grep -v grep|awk '{print $2}'" };
			Process process = Runtime.getRuntime().exec(cmds);
			List<String> resultList = IOUtils.readLines(process.getInputStream());
			if (resultList == null || resultList.size() == 0) {
				return false;
			}
			String pidStr = resultList.get(0);
			logger.info("PID {} STR {}", pid, pidStr);
			Integer spid = Integer.parseInt(pidStr);
			return spid.equals(pid);
		} catch (IOException e) {
			logger.error("查找进程失败", e);
		}
		// catch (InterruptedException e) {
		// logger.error("查找进程失败", e);
		// }
		return false;
	}

	@Override
	public Integer findPid(String md5) {
		try {
			Integer pid = null;
			String[] cmds = { "/bin/sh", "-c", "ps aux|grep " + md5 + "|grep -v grep|awk '{print $2}'" };
			Process process = Runtime.getRuntime().exec(cmds);
			List<String> resultList = IOUtils.readLines(process.getInputStream());
			if (resultList == null || resultList.size() == 0) {
				return null;
			}
			String pidStr = resultList.get(0);
			logger.info("PID {} STR {}", pid, pidStr);
			Integer spid = Integer.parseInt(pidStr);
			return pid;
		} catch (IOException e) {
			logger.error("查找进程失败", e);
		}
		return null;
	}

	// @Override
	// public Integer exists(Integer pid, String folder) {
	// try {
	// File file = new File(FileNameUtil.normalize(folder + "/", true));
	// Process process = Runtime.getRuntime().exec("sh exists.sh", null, file);
	// List<String> result = IOUtils.readLines(process.getInputStream());
	// if (CollectionUtils.isNotEmpty(result)) {
	// pid = NumberUtils.toInt(result.get(0));
	// return pid == 0 ? null : NumberUtils.toInt(result.get(0));
	// }
	// } catch (Exception e) {
	// logger.error("查找进程失败", e);
	// }
	// return null;
	// }

	@Override
	public boolean kill(Integer pid) {
		try {
			Process process = Runtime.getRuntime().exec("kill -9 " + pid);
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("结束进程失败", e);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean kill(Integer pid, String folder) {
		try {
			File file = new File(FileNameUtil.normalize(folder + "/", true));
			Process process = Runtime.getRuntime().exec("sh stop.sh", null, file);
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("结束进程失败", e);
		}
		return false;
	}

	@Override
	public boolean start(String folder) {
		logger.info("开启进程{}", folder);
		try {
			File file = new File(FileNameUtil.normalize(folder + "/", true));
			Process process = Runtime.getRuntime().exec("sh start.sh", null, file);
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("开启进程失败", e);
		}
		logger.info("开启进程结束{}", folder);
		return false;
	}

}

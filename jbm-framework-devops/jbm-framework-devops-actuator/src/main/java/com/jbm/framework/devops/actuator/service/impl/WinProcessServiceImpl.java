package com.jbm.framework.devops.actuator.service.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jbm.framework.devops.actuator.config.SigarService;
import com.jbm.framework.devops.actuator.service.ProcessService;

//@Service
public class WinProcessServiceImpl implements ProcessService {
	private final static Logger logger = LoggerFactory.getLogger(WinProcessServiceImpl.class);

	private SigarService sigarService;

	public WinProcessServiceImpl(SigarService sigarService) {
		super();
		this.sigarService = sigarService;
	}

	public boolean exists(Integer pid) {
		try {
			if (pid == null)
				return false;
			if (sigarService != null)
				return sigarService.existsPid(pid);
			Process process = Runtime.getRuntime().exec("cmd /c tasklist |findstr " + pid);
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("查找进程失败", e);
		} finally {
			logger.error("查找进程完成{}", pid);
		}
		return false;
	}

	// public Integer exists(Integer pid, String folder) {
	// try {
	// Process process = Runtime.getRuntime().exec("cmd /c exists.bat", null,
	// new File(folder));
	// List<String> result = IOUtils.readLines(process.getInputStream());
	// if (CollectionUtils.isNotEmpty(result)) {
	// pid = NumberUtils.toInt(result.get(0));
	// return pid == 0 ? null : NumberUtils.toInt(result.get(0));
	// }
	// } catch (Exception e) {
	// logger.error("查找进程失败", e);
	// } finally {
	// logger.info("查找进程完成{}", folder);
	// }
	// return null;
	// }

	public boolean kill(Integer pid) {
		try {
			if (!sigarService.existsPid(pid))
				return true;
			Process process = Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("结束进程失败", e);
			e.printStackTrace();
		} finally {
			logger.info("结束进程完成{}", pid);
		}
		return false;
	}

	public boolean kill(Integer pid, String folder) {
		try {
			// Process process = Runtime.getRuntime().exec("taskkill /F /T /PID
			// " + pid);
			Process process = Runtime.getRuntime().exec("cmd /c stop.bat", null, new File(folder));
			return !IOUtils.readLines(process.getInputStream()).isEmpty();
		} catch (IOException e) {
			logger.error("结束进程失败", e);
		} finally {
			logger.info("结束进程完成{}", folder);
		}
		return false;
	}

	public boolean start(String folder) {
		logger.info("开启进程{}", folder);
		try {
			Process process = Runtime.getRuntime().exec("cmd /c start.bat", null, new File(folder));
			process.waitFor();
			int i = process.exitValue();
			return i == 0;
		} catch (Exception e) {
			logger.error("开启进程失败", e);
		} finally {
			logger.info("开启进程完成{}", folder);
		}
		return false;
	}

	@Override
	public Integer findPid(String md5) {
		return null;
	}

}

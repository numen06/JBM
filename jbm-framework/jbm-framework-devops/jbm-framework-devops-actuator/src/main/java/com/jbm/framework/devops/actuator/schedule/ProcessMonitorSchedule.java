/**
 * 
 */
package com.jbm.framework.devops.actuator.schedule;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.jbm.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.jbm.framework.devops.actuator.service.ProcessService;
import com.jbm.framework.devops.actuator.service.ProgramInfoService;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.util.FileUtils;
import com.jbm.util.MD5Utils;
import com.jbm.util.MapUtils;

import jbm.framework.boot.autoconfigure.rest.RealRestTemplate;
import jbm.framework.boot.autoconfigure.rest.RestTemplateFactory;

/**
 * @author wesley
 *
 */
public class ProcessMonitorSchedule extends AbstractScheduledService {

	private static Logger logger = LoggerFactory.getLogger(ProcessMonitorSchedule.class);

	private RealRestTemplate realRestTemplate = RestTemplateFactory.getInstance().createRealRestTemplate();

	private long initialDelay = 5l;

	private long delay = 5l;

	private TimeUnit unit = TimeUnit.SECONDS;
	// // 进程id
	// private int pid = 0;

	private Properties systemProperties;

	private Properties env;

	private ProgramInfo programInfo;

	/**
	 * 开启保护
	 */
	private int startSoftProtection = 0;

	/**
	 * 关闭保护
	 */
	private int killSoftProtection = 0;

	/**
	 * 异常启动次数
	 */
	private int abnormalTime = 0;

	// /**
	// * 进程是否异常
	// */
	// private boolean offLine = true;

	// private ProgramServiceImpl programServiceImpl = null;

	private ProgramInfoService programInfoService;

	private ProcessService processService;

	/**
	 * 0:未启动， 1:启动中，2:运行中，3：正在关闭，4:正常关闭
	 */
	private int mode = 0;

	public ProcessMonitorSchedule(ProgramInfo programInfo, ProgramInfoService programInfoService, ProcessService processService) throws Exception {
		super();
		this.programInfo = programInfo;
		this.programInfoService = programInfoService;
		this.processService = processService;

		if (programInfo == null) {
			throw new NullPointerException("programInfo is null");
		}
		if (programInfo.getFolder() == null) {
			throw new NullPointerException("main file is null");
		} else {
			if (!FileUtils.exists(new File(programInfo.getFolder()))) {
				throw new NullPointerException("folder is not exists");
			}
		}
		if (programInfo.getMport() == null) {
			throw new NullPointerException("mport is null");
		}
		try {
			this.initializePid();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		// this.programInfo.getPid();
	}

	@Override
	protected void runOneIteration() throws Exception {
		// 启停保护
		softProtection();
		try {
			if (this.programInfo.getId() != null)
				this.programInfo = this.programInfoService.findProgrameInfoById(this.programInfo.getId());
			this.runDetection();
			// 异常处理
			abnormalDo();
		} catch (Exception e) {
			logger.error("守护线程错误", e);
		}
	}

	/**
	 * 异常处理
	 */
	private synchronized void abnormalDo() throws Exception {
		if (this.mode == 4) {
			if (abnormalTime > 2) {
				this.stopAsync();
				return;
			}
			logger.info("检测到程序[{}]异常,PID:[{}]下线,正在重启程序", programInfo.getFolder(), this.programInfo.getPid());
			// this.mode = 4;
			this.programInfo.setEndTime(new Date());
			// this.programInfo.setStauts(false);
			this.startProcess();
			abnormalTime++;
			this.programInfo.setPid(null);
		} else {
			this.programInfo.setStauts(true);
			// abnormalTime = 0;
			logger.info("程序[{}],PID {} state {},Thread state {}", programInfo.getFolder(), this.programInfo.getPid(), this.mode, this.state());
		}
	}

	/**
	 * 启停保护
	 */
	private synchronized void softProtection() {
		if (this.mode == 0 || this.mode == 1 || this.mode == 2) {
			if (startSoftProtection < 3) {
				startSoftProtection++;
			}
		} else if (this.mode == 3 || this.mode == 4) {
			if (killSoftProtection < 3) {
				killSoftProtection++;
			}
		}
	}

	/**
	 * 运行监测
	 */
	private void runDetection() {
		String result = getRequestBody("metrics");
		// logger.info("pinfo {}", result);
		if (result == null) {
			if (this.mode == 1) {
				if (startSoftProtection > 2) {
					if (!this.initializePid()) {
						logger.info("程序[Process:{}]检测PID未果，检测到异常关闭。", programInfo.getFolder());
						this.mode = 4;
					} else {
						if (!this.checkExists()) {
							this.mode = 4;
						}
					}
				}
			} else if (this.mode == 2) {
				// if (!this.checkExists()) {
				this.mode = 4;
				// }
			} else if (this.mode == 3) {
			} else if (this.mode == 4) {
			} else {
				if (startSoftProtection > 2) {
					this.mode = 4;
				}
			}
		} else {
			if (mode == 1) {
				this.initializePid();
			}
			abnormalTime = 0;
			this.mode = 2;
		}
	}

	/**
	 * 确定是否存在线程
	 * 
	 * @return
	 */
	private synchronized boolean checkExists() {
		int count = 0;
		if (this.programInfo.getPid() == null)
			return false;
		for (int i = 0; i < 1; i++) {
			if (this.processService.exists(this.programInfo.getPid())) {
				return true;
			} else {
				count++;
			}
		}
		if (count > 1) {
			logger.info("程序[Process:{}]已关闭", programInfo.getFolder());
			// this.mode = 4;
			return false;
		}
		return false;
	}

	/**
	 * 获取JMX监控信息
	 * 
	 * @param path
	 * @return
	 */
	private String getRequestBody(String path) {
		if (this.programInfo.getMport() == null) {
			logger.error("监控端口为空");
		}
		String url = "http://127.0.0.1:" + this.programInfo.getMport() + "/" + path;
		try {
			ResponseEntity<String> result = realRestTemplate.getForEntity(url, String.class, MapUtils.newParamMap());
			logger.trace(result.toString());
			// System.out.println(result.toString());
			return result.getBody();
		} catch (RestClientException e) {
			return null;
		}
	}

	/**
	 * 重新设置PID
	 * 
	 * @return
	 */
	private boolean initializePid() {
		try {
			String result = getRequestBody("env");
			if (result == null) {
				findPidFile();
				if (this.programInfo.getPid() == null) {
					this.programInfo.setPid(null);
					logger.info("程序[Process:" + programInfo.getFolder() + "]未找到PID");
				} else {
					this.programInfo.setActivationTime(new Date());
				}
			} else {
				this.env = JSON.parseObject(result, Properties.class);
				this.systemProperties = JSON.parseObject(this.env.get("systemProperties").toString(), Properties.class);
				this.programInfo.setPid(Integer.parseInt(systemProperties.getProperty("PID")));
				this.programInfo.setActivationTime(new Date());
			}
		} catch (Exception e) {
			logger.error("程序[Process:" + programInfo.getFolder() + "]未找到PID");
			return false;
		} finally {
			refProgramInfo(true);
		}
		return true;
	}

	/**
	 * 查找PID文件
	 * 
	 * @return
	 */
	private synchronized void findPidFile() throws ServiceException {
		if (this.programInfo.getPid() != null) {
			if (!this.checkExists()) {
				this.programInfo.setPid(null);
			}
		}
		try {
			Integer pid = null;
			String md5 = MD5Utils.MD5(this.programInfo.getFolder());
			pid = this.processService.findPid(md5);
			if (pid == null) {
				File[] temp = FileUtils.filesMatchingStemRegex(new File(this.programInfo.getFolder()), ".*.pid");
				if (temp.length == 1) {
					pid = Integer.parseInt(FileUtils.readFileToString(temp[0]));
					this.programInfo.setPid(pid);
				} else if (temp.length > 1) {
					for (int i = 0; i < temp.length; i++) {
						FileUtils.forceDelete(temp[i]);
					}
					throw new ServiceException("存在多个PID文件");
				} else {
					throw new ServiceException("不存在PID文件");
				}
			} else {
				this.programInfo.setPid(pid);
			}
		} catch (Exception e) {
			logger.error("查找PID错误[Process:" + programInfo.getFolder() + "]", e);
			throw new ServiceException("查找PID错误");
		}
	}

	/**
	 * 开启线程
	 * 
	 * @return
	 */
	public synchronized void startProcess() throws ServiceException {
		// 线程是否启动
		boolean pStart = false;
		// 守护是否启动
		boolean mStart = false;
		if (this.programInfo == null) {
			logger.error("程序信息为空");
			throw new ServiceException("程序信息为空");
		}
		if (StringUtils.isBlank(this.programInfo.getFolder())) {
			logger.error("主程序路径为空");
			throw new ServiceException("主程序路径为空");
		}
		if (this.isRunning()) {
			mStart = true;
			if (this.mode == 1) {
				logger.info("程序[{}]正在启动", programInfo.getFolder());
				throw new ServiceException("程序正在启动");
			}
			if (this.mode == 2) {
				logger.info("程序[{}]已经运行", programInfo.getFolder());
				throw new ServiceException("程序已经运行");
			}
			if (this.mode == 3) {
				logger.info("程序[{}]正在关闭", programInfo.getFolder());
				throw new ServiceException("程序正在关闭");
			}
			if (this.checkExists()) {
				logger.info("程序[{}]进程正在运行", programInfo.getFolder());
				pStart = true;
				// throw new ServiceException("进程正在运行");
			} else {
				pStart = false;
			}
		} else {
			mStart = false;
			if (this.checkExists()) {
				logger.info("程序[{}]进程正在运行", programInfo.getFolder());
				pStart = true;
			} else {
				pStart = false;
			}
		}
		if (!mStart) {
			// // 设置启动模式
			// this.mode = 1;
			// 放入线程池
			ScheduleTaskPool.put(this.programInfo.getFolder(), this);
			// 启动守护线程
			this.startAsync().awaitRunning();
			mStart = true;
		}
		if (!pStart && mStart) {
			if (this.mode == 0 || this.mode == 4) {
				// 设置启动模式
				this.mode = 1;
				this.startSoftProtection = 0;
				// 启动程序
				processService.start(this.programInfo.getFolder());
				// 更新启动信息资料
				this.programInfo.setStartTime(new Date());
				this.refProgramInfo(true);
			}
		}
	}

	/**
	 * 停止程序
	 * 
	 * @return
	 */
	public synchronized void stopProcess() throws ServiceException {
		if (startSoftProtection < 3)
			throw new ServiceException("启动保护中，请稍后再试 ");
		if (this.programInfo == null) {
			logger.error("程序信息为空");
			throw new ServiceException("程序信息为空 ");
		}
		if (StringUtils.isBlank(this.programInfo.getFolder())) {
			logger.error("主程序路径为空");
			throw new ServiceException("主程序路径为空");
		}
		// 如果已经关闭返回
		if (this.mode == 1 && this.startSoftProtection < 3) {
			logger.info("程序[{}]正在启动中", programInfo.getFolder());
			throw new ServiceException("程序正在启动中");
		}
		// 如果已经关闭返回
		if (this.mode == 3 && this.killSoftProtection < 3) {
			logger.info("程序[{}]正在关闭", programInfo.getFolder());
			throw new ServiceException("程序正在关闭");
		}
		if (this.mode == 4) {
			logger.info("程序[{}]已经关闭", programInfo.getFolder());
			throw new ServiceException("程序已经关闭");
		}
		try {
			// 设置模式
			this.mode = 3;
			// 关闭程序
			if (this.programInfo.getPid() == null) {
				logger.info("程序[{}],PID为空,尚未启动或监控", programInfo.getFolder());
				throw new ServiceException("程序PID为空,尚未启动或监控");
			} else {
				for (int i = 0; i < 3; i++) {
					processService.kill(this.programInfo.getPid());
					if (!processService.exists(this.programInfo.getPid())) {
						break;
					}
				}
			}
		} finally {
			// 停止守护进程
			this.stopAsync();
		}
	}

	/**
	 * 刷新程序信息
	 */
	private void refProgramInfo(Boolean run) {
		try {
			// this.programInfo =
			// this.programInfoService.selectByPrimaryKey(this.programInfo.getId());
			// this.programInfo.setPid(this.pid);
			// this.programInfo.setStauts(this.programInfo.getPid() != null);
			if (run == null)
				run = true;
			this.programInfo.setStauts(run);
			this.programInfoService.saveProgrameInfo(this.programInfo);
		} catch (ServiceException e) {
			logger.error("刷写程序[Process:{}]信息错误", programInfo.getFolder(), e);
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(initialDelay, delay, unit);
	}

	public long getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	@Override
	protected String serviceName() {
		return this.programInfo.getFolder();
	}

	@Override
	protected void startUp() throws Exception {
		logger.info("守护线程[Process:{}]启动", programInfo.getFolder());
		this.programInfo.setStauts(true);
		this.refProgramInfo(true);
		super.startUp();
	}

	@Override
	protected void shutDown() throws Exception {
		logger.info("守护线程[Process:{}]关闭", programInfo.getFolder());
		this.programInfo.setEndTime(new Date());
		this.programInfo.setStauts(false);
		this.programInfo.setPid(null);
		this.refProgramInfo(false);
		ScheduleTaskPool.remove(this.programInfo.getFolder());
		super.shutDown();
	}

	// public static void main(String[] args) {
	// File[] temp = FileUtils.filesMatchingStemRegex(new
	// File("D:/maven_workspaces/td-framework/td-framework-devops/td-framework-devops-actuator"),
	// ".*.pid");
	// System.out.println(JSON.toJSONString(temp));
	// }

}

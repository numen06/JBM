package com.td.framework.devops.actuator.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.SocketUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.td.framework.devops.actuator.bean.RecoveryInfo;
import com.td.framework.devops.actuator.common.ActuatorConfig;
import com.td.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.td.framework.devops.actuator.schedule.ProcessMonitorSchedule;
import com.td.framework.devops.actuator.schedule.ScheduleTaskPool;
import com.td.framework.devops.actuator.service.ProcessService;
import com.td.framework.devops.actuator.service.ProgramInfoService;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.util.ArrayUtils;
import com.td.util.FileUtils;
import com.td.util.ListUtils;
import com.td.util.MD5Utils;
import com.td.util.MapUtils;
import com.td.util.StringUtils;
import com.td.util.TemplateUtils;
import com.td.util.TimeUtil;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import jodd.util.StringUtil;

/**
 * @author wesley
 *
 */
@Service
public class ProgramServiceImpl {
	private final static Logger logger = LoggerFactory.getLogger(ProgramServiceImpl.class);
	@Autowired
	private ProcessService processService;
	@Autowired
	private ProgramInfoService programInfoService;

	public void setProcessService(ProcessService processService) {
		this.processService = processService;
	}

	@Autowired
	private ActuatorConfig actuatorConfig;

	@Autowired
	private ApplicationContext applicationContext;

	@SuppressWarnings("unchecked")
	public void reduction(String recoveryPath, String sourcePath, boolean replace) throws IOException, ServiceException {
		logger.info("recoveryPath {}", recoveryPath);
		logger.info("sourcePath {}", sourcePath);
		File recoveryDir = new File(recoveryPath);
		if (!recoveryDir.exists() || !recoveryDir.isDirectory()) {
			throw new IOException(MessageFormat.format("{0} is not directory", recoveryPath));
		}
		File sourceDir = new File(sourcePath);
		if (!sourceDir.exists() || !sourceDir.isDirectory()) {
			throw new IOException(MessageFormat.format("{0} is not directory", sourcePath));
		}
		File[] files = FileUtils.filesMatchingStemRegex(recoveryDir, ".*.recovery");
		File recoveryFile = files[0];
		String recoveryInfo = FileUtils.readFileToString(recoveryFile);
		Map<String, String> map = JSON.parseObject(recoveryInfo, Map.class);

		// 是否替换
		File tempSource = new File(FileNameUtil.concat(sourceDir.getPath(), map.get("sourceName")));
		if (FileUtils.exists(tempSource.getPath()) && !replace) {
			throw new ServiceException("此文件在资源目录中已存在");
		} else if (FileUtils.exists(tempSource.getPath()) && replace) {
			// 进行替换操作
			ProgramInfo info = programInfoService.buildProgramInfo(tempSource);
			if (info.getStauts()) {
				throw new ServiceException("要替换的目录正在启动中，请先关闭");
			}
		}
		File sourceFile = new File(FileNameUtil.concat(recoveryDir.getPath(), map.get("sourceName")));
		FileUtils.moveDirectoryToDirectory(sourceFile, sourceDir, true);
		FileUtils.deleteDirectory(recoveryDir);
	}

	public void recoveryProcess(String filepath) throws ServiceException {
		if (this.existProgram(filepath)) {
			throw new ServiceException("请先关闭程序");
		}
		File file = new File(filepath);
		if (!FileUtil.isExistingFolder(file)) {
			this.programInfoService.deleteProgrameInfo(filepath);
			return;
		}
		try {
			long uid = System.currentTimeMillis();
			File recoveryDir = new File(actuatorConfig.getProcessRecoveryPath() + uid);
			FileUtils.moveDirectoryToDirectory(file, recoveryDir, true);
			File infoFile = new File(recoveryDir.getPath() + "/" + uid + ".recovery");
			Map<String, Object> map = MapUtils.newHashMap();
			map.put("filename", file.getName());
			map.put("time", new Date());
			map.put("dir", FileNameUtil.getPath(filepath));
			map.put("sourceName", file.getName());
			map.put("type", "process");
			FileUtils.writeStringToFile(infoFile, JSON.toJSONString(map));
			this.programInfoService.deleteProgrameInfo(filepath);
		} catch (Exception e) {
			throw new ServiceException("回收进程错误", e);
		}
	}

	// public static void main(String[] args) {
	// String str = "target/release/td-dpen-center-web/default";
	// File file = new File(str);
	// System.out.println(FilenameUtils.getName(file.getParent()));
	// }

	public DataPaging<ProgramInfo> searchProgramList(String mainFileName, Boolean status) throws ServiceException {
		// DataPaging<ProgramInfo> page =
		// this.programInfoService.findAllProgrameList();
		// if (StringUtils.isBlank(mainFileName) && status == null) {
		// return page;
		// }
		List<ProgramInfo> list = this.programInfoService.findAllProgrameList();
		DataPaging<ProgramInfo> dataPaging = new DataPaging<ProgramInfo>(list, list.size());
		return dataPaging;
	}

	public DataPaging<RecoveryInfo> findRecoveryProgramList(String filename, String type) throws IOException {
		String rootpath = null;

		if (StringUtils.isBlank(type)) {
			rootpath = actuatorConfig.getRecoveryPath();
		} else {
			if (type.equals("process")) {
				rootpath = actuatorConfig.getProcessRecoveryPath();
			} else if (type.equals("source")) {
				rootpath = actuatorConfig.getSourceRecoveryPath();
			} else {
				rootpath = actuatorConfig.getRecoveryPath();
			}
		}
		File file = new File(rootpath);
		File[] files = FileUtils.filesMatchingStemRegex(file, ".*.recovery");

		List<RecoveryInfo> list = toRecoveryProcessInfo(files);

		if (!StringUtils.isBlank(filename) && list != null && list.size() > 0) {
			List<RecoveryInfo> filterlist = ListUtils.newArrayList();
			for (RecoveryInfo info : list) {
				if (info.getSourceName().indexOf(filename) != -1) {
					filterlist.add(info);
				}
			}
			list = filterlist;
		}
		Collections.sort(list, new Comparator<RecoveryInfo>() {
			@Override
			public int compare(RecoveryInfo o1, RecoveryInfo o2) {
				return o1.getRecoveryDate().after(o2.getRecoveryDate()) ? -1 : 1;
			}
		});
		DataPaging<RecoveryInfo> dataPaging = new DataPaging<RecoveryInfo>(list, list.size());
		return dataPaging;
	}

	public List<RecoveryInfo> toRecoveryProcessInfo(File[] files) throws IOException {
		List<RecoveryInfo> programList = ListUtils.newArrayList();
		RecoveryInfo programInfo = null;
		for (File file : files) {
			programInfo = toRecoveryProcess(file);
			programList.add(programInfo);
		}
		return programList;
	}

	@SuppressWarnings("rawtypes")
	public RecoveryInfo toRecoveryProcess(File file) throws IOException {
		RecoveryInfo info = new RecoveryInfo();
		final String folder = FileNameUtil.normalize(file.getParentFile().getPath(), true);
		info.setFolder(folder);
		String fileJson = FileUtils.readFileToString(file);
		Map map = JSON.parseObject(fileJson, Map.class);
		info.setSourcePath(map.get("dir").toString());
		info.setMianFile((String) map.get("filename"));
		Long time = Long.parseLong(map.get("time").toString());
		Date date = new Date(time);
		info.setSourceName(map.get("sourceName").toString());
		info.setType(map.get("type").toString());
		info.setCreateTime(date);
		info.setRecoveryDate(date);
		return info;
	}

	public DataPaging<ProgramInfo> scanningProgramList() {
		return scanningProgramList(null);
	}

	private String concatPath(Object... path) {
		return FileNameUtil.normalize(StringUtil.join(path, "/"), true);
	}

	public void toBuckup(String folder) throws ServiceException {
		try {
			ProgramInfo info = programInfoService.findProgrameInfo(folder);
			if (info == null) {
				return;
			} // 备份文件信息
			if (info.getStauts()) {
				throw new ServiceException("进程正在运行无法备份");
			}
			if (this.existProgram(folder)) {
				throw new ServiceException("进程正在运行无法备份");
			}
			String bakFolderPath = concatPath(folder, "bak", TimeUtil.format(TimeUtil.now(), "yyyy-MM-dd_HH-mm-ss"));
			File bakFolder = new File(bakFolderPath);
			if (!bakFolder.exists()) {
				bakFolder.mkdir();
			}
			File jarFile = new File(concatPath(info.getFolder(), info.getMianFile()));
			if (!jarFile.exists()) {
				return;
			}
			File configDir = new File(concatPath(info.getFolder(), "config"));
			if (configDir.exists() && configDir.isDirectory()) {
				FileUtils.copyDirectoryToDirectory(configDir, bakFolder);
			}
			FileUtils.copyFileToDirectory(jarFile, bakFolder);
		} catch (IOException e) {
			throw new ServiceException("备份错误", e);
		}
	}

	/**
	 * @return
	 */
	public DataPaging<ProgramInfo> scanningProgramList(String jarPath) {
		if (StringUtils.isBlank(jarPath)) {
			jarPath = actuatorConfig.getSearchPath();
		}
		String[] searchPaths = StringUtils.splitToEmpty(jarPath, ",");
		if (!ArrayUtils.contains(searchPaths, actuatorConfig.getReleasePath()))
			searchPaths = (String[]) ArrayUtils.add(searchPaths, actuatorConfig.getReleasePath());
		List<File> files = new ArrayList<>();
		for (int i = 0; i < searchPaths.length; i++) {
			String path = searchPaths[i];
			if (StringUtils.isBlank(path))
				continue;
			files.addAll(Lists.newArrayList(FileUtils.filesMatchingStemRegex(new File(path), "((?!/bak/).)*.jar")));
		}
		DataPaging<ProgramInfo> dataPaging = new DataPaging<>();
		try {
			List<ProgramInfo> programInfos = programInfoService.buildProgramInfo(files);
			dataPaging = new DataPaging<ProgramInfo>(programInfos, programInfos.size());
		} catch (ServiceException e) {
			logger.error("查找程序列表错误", e);
		}
		return dataPaging;
	}

	public Map<String, String> getGcInfoMap(String folder) throws NumberFormatException, IOException, ServiceException {
		File[] pidFiles = FileUtils.filesMatchingStemRegex(new File(folder), ".*.pid");
		if (ArrayUtils.isNotEmpty(pidFiles)) {
			String pidStr = FileUtils.readFileToString(pidFiles[0]);
			if (StringUtils.isBlank(pidStr)) {
				throw new ServiceException("当前进程未开启");
			}
		}
		return null;
	}

	public void killProgram(String folder) throws ServiceException {
		ProcessMonitorSchedule processMonitorSchedule = ScheduleTaskPool.get(folder);
		if (processMonitorSchedule == null) {
			logger.info("程序[{}]没有运行", folder);
			// throw new ServiceException("程序没有运行");
		} else {
			processMonitorSchedule.stopProcess();
			processMonitorSchedule.awaitTerminated();
		}
	}

	public void startProgram(File jar) throws ServiceException {
		ProgramInfo info = this.programInfoService.buildProgramInfo(jar);
		startProgram(info.getFolder());

	}

	public void startProgram(String folder) throws ServiceException {
		// startProgram(folder, true);
		try {
			ProcessMonitorSchedule processMonitorSchedule = ScheduleTaskPool.get(folder);
			if (processMonitorSchedule == null) {
				// 初始化启动文件
				ProgramInfo info = this.programInfoService.findProgrameInfo(folder);
				// 获取监控端口
				if (info.getMport() == null) {
					logger.info("程序[{}]未初始化", folder);
					structure(info);
				}
				processMonitorSchedule = new ProcessMonitorSchedule(info, programInfoService, processService);
			}
			processMonitorSchedule.startProcess();
			processMonitorSchedule.awaitRunning();
		} catch (ServiceException e) {
			logger.error("启动程序[{}]失败", folder, e);
			throw e;
		} catch (Exception e) {
			logger.error("启动程序[{}]失败", folder, e);
			throw new ServiceException(e);
		}
	}

	public boolean existProgram(String folder) throws ServiceException {
		ProcessMonitorSchedule processMonitorSchedule = ScheduleTaskPool.get(folder);
		if (processMonitorSchedule == null) {
			return false;
		}
		return true;
	}

	/**
	 * 构造相应的文件信息
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void structure(ProgramInfo info) throws IOException {
		// String path = new File(file.toURI()).getParent();
		try {
			String path = info.getFolder();
			// String fileName = file.getName();
			String fileName = info.getMianFile();
			if (!StringUtils.isAllNotBlank(path, fileName)) {
				throw new ServiceException("没有主路径和程序名称");
			}
			// File startFile = new File(FileNameUtil.concat(path, "start.bat",
			// true));
			// if (FileUtils.exists(startFile))
			// FileUtils.forceDelete(startFile);
			Map<String, Object> params = Maps.newHashMap();
			String md5 = MD5Utils.MD5(path);
			params.put("title", MessageFormat.format("{0}_{1}_{2}", path, FileNameUtil.getBaseName(fileName), md5));
			params.put("jar", fileName);
			Integer mport = SocketUtils.findAvailableTcpPort();
			params.put("tid", md5);
			params.put("args", Lists.newArrayList("--management.port=" + mport, "--management.security.enabled=false", "--management.address=127.0.0.1"));
			// String startValue = //
			// TemplateUtils.renderTemplate(applicationContext.getResource("classpath:command/win/start.txt").getInputStream(),
			// params);
			// FileUtils.writeLines(startFile, Lists.newArrayList(startValue));
			// 开始文件
			writeTemple(path, "start", params);
			// 关闭文件
			writeTemple(path, "stop", params);
			// 是否存在
			writeTemple(path, "exists", params);

			info = programInfoService.findProgrameInfo(path);
			if (info == null) {
				programInfoService.buildProgramInfo(new File(FileNameUtil.concat(path, fileName)));
			}
			info.setMport(mport);
			programInfoService.saveProgrameInfo(info);
		} catch (ServiceException e) {
			logger.error("初始化程序信息失败", e);
		}
		// exec("cmd /c cd source && start.bat");
		// SpringApplication.exit(applicationContext);
	}

	private void writeTemple(String path, String tempname, Map<String, Object> params) throws IOException {
		if (ActuatorConfig.isWindows()) {
			File stopFile = new File(FileNameUtil.concat(path, MessageFormat.format("{0}.bat", tempname), true));
			if (FileUtils.exists(stopFile))
				FileUtils.forceDelete(stopFile);
			String stopValue = TemplateUtils.renderTemplate(applicationContext.getResource(MessageFormat.format("classpath:command/win/{0}.txt", tempname)).getInputStream(),
				params);
			FileUtils.writeLines(stopFile, Lists.newArrayList(stopValue));
		} else {
			File stopFile = new File(FileNameUtil.concat(path, MessageFormat.format("{0}.sh", tempname), true));
			if (FileUtils.exists(stopFile))
				FileUtils.forceDelete(stopFile);
			String stopValue = TemplateUtils.renderTemplate(applicationContext.getResource(MessageFormat.format("classpath:command/linux/{0}.txt", tempname)).getInputStream(),
				params);
			FileUtils.writeLines(stopFile, Lists.newArrayList(stopValue));
		}
	}

	public ProgramInfo refreshProgram(ProgramInfo info) throws ServiceException {
		return this.programInfoService.findProgrameInfo(info.getFolder());
	}

}

package com.td.framework.devops.actuator.service;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.td.devops.bean.ReleaseOption;
import com.td.devops.util.MD5Util;
import com.td.framework.devops.actuator.bean.FileInfo;
import com.td.framework.devops.actuator.bean.SourceInfo;
import com.td.framework.devops.actuator.common.ActuatorConfig;
import com.td.framework.devops.actuator.service.impl.FolderFileServiceImpl;
import com.td.framework.devops.actuator.service.impl.ProgramServiceImpl;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.page.DataPaging;
import com.td.util.FileUtils;
import com.td.util.ListUtils;
import com.td.util.MapUtils;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import jodd.io.ZipUtil;
import jodd.util.StringUtil;

@Service
public class SourceService {
	private final static Logger logger = LoggerFactory.getLogger(SourceService.class);
	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private ActuatorConfig actuatorConfig;

	@Autowired
	private ProgramServiceImpl programServiceImpl;

	@Autowired
	private FolderFileServiceImpl folderFileServiceImpl;

	// @PostConstruct
	public void init() throws IOException, InterruptedException {
		loadSource(null);
	}

	@Value("${doc.url}")
	private String docUrl;

	public boolean isRepeatFile(String fileMd5, String filename) throws IOException, ServiceException {
		if (StringUtils.isBlank(fileMd5)) {
			throw new IllegalArgumentException("fileMd5 is blank");
		}
		if (StringUtils.isBlank(filename)) {
			throw new IllegalArgumentException("filename is blank");
		}
		String sourceName = FileNameUtil.getBaseName(filename);
		String path = actuatorConfig.getSourcePath() + "/" + sourceName;
		// String format = FileNameUtil.getExtension(filename);
		File[] files = FileUtils.filesMatchingStemRegex(new File(path), ".*.info");
		if (this.sourceExistByInfo(fileMd5, files)) {
			return true;
		}
		return false;
	}

	public void publish(ReleaseOption option) throws IOException, ServiceException {
		// for (FileInfoBean bean : option.getFileInfos()) {
		// String extension = FileNameUtil.getExtension(bean.getFileName());
		// String url = MessageFormat.format(docUrl + "/doc/{0}.{1}",
		// bean.getId(), extension);
		// logger.info("download file url [{}]", url);
		// HttpRequest httpRequest = HttpRequest.get(url);
		// HttpResponse response = httpRequest.send();
		// // String filanme = response.header("content-disposition");
		// this.saveChannelSource(bean.getFileName(), response.bodyBytes(),
		// option);
		// }
		for (String url : option.getUrls()) {
			logger.info("download file url [{}]", url);
			HttpRequest httpRequest = HttpRequest.get(url).basicAuthentication("tdadmin", "Td57871528");
			HttpResponse response = httpRequest.send();
			option.setStart(true);
			String fileName = StringUtils.substringAfterLast(url, "/");
			this.saveChannelSource(fileName, response.bodyBytes(), option);
		}
	}

	public String saveChannelSource(String originalFilename, byte[] bytes, ReleaseOption option) throws IOException, ServiceException {
		String infoPath = this.saveSource(originalFilename, bytes);
		String infoName = FileNameUtil.getBaseName(infoPath);
		String path = FileNameUtil.getPathNoEndSeparator(infoPath);
		String releasePath = null;
		if (option.getStart()) {
			this.releaseDesignatedDirStart(path, infoName, option.getDirname(), true);
			// releasePath = this.releaseStart(infoFileName);
		} else if (option.getRelease()) {
			releasePath = this.release(infoName);
		}
		return releasePath;
	}

	public List<FileInfo> getSourceDir() throws ServiceException {
		String path = actuatorConfig.getSourcePath();
		File sourceDir = new File(path);
		List<FileInfo> list = ListUtils.newArrayList();
		FileInfo info = null;
		if (!sourceDir.exists())
			return list;
		for (File file : sourceDir.listFiles()) {
			if (file.isDirectory()) {
				info = folderFileServiceImpl.convertFileInfo(file);
				list.add(info);
			}
		}
		return list;
	}

	public String saveSource(String originalFilename, byte[] bytes) throws IOException, ServiceException {
		String sourceName = FileNameUtil.getBaseName(originalFilename);
		originalFilename = timeStampFilename(originalFilename);
		String path = concatPath(actuatorConfig.getSourcePath(), sourceName);
		File file = new File(FileNameUtil.concat(path, originalFilename, true));
		// String format = FileNameUtil.getExtension(originalFilename);
		// File[] files = FileUtils.filesMatchingStemRegex(new File(path), ".*."
		// + format);
		// String fileMd5 = MD5Util.getMD5String(bytes);

		// if (sourceExist(fileMd5, files)) {
		// throw new ServiceException(MessageFormat.format("文件包 {0} 已存在",
		// originalFilename));
		// }
		FileUtils.writeByteArrayToFile(file, bytes);
		String uuid = UUID.randomUUID().toString();

		File sourceInfo = new File(FileNameUtil.concat(path, uuid + ".info", true));
		String sourcePath = FileNameUtil.concat(path, originalFilename, true);
		FileUtils.writeStringToFile(sourceInfo, sourcePath);
		saveLast(path, sourcePath, uuid);
		return sourceInfo.getPath();
	}

	private static final String SOURCE_LAST = "source.last";

	public void saveLast(String path, String sourcePath, String md5) throws IOException {
		File last = new File(FileNameUtil.concat(path, SOURCE_LAST));
		List<String> lines = ListUtils.newArrayList(sourcePath, md5);
		// FileUtils.writeLines(last, lines);
		FileUtils.writeLines(last, lines, true);
	}

	public boolean sourceExistByInfo(String fileMd5, File[] infoFiles) throws IOException {
		for (File file : infoFiles) {
			if (fileMd5.equals(FileNameUtil.getBaseName(file.getName()))) {
				return true;
			}
		}
		return false;
	}

	public boolean sourceExist(String fileMd5, File[] files) throws IOException {
		for (File file : files) {
			// b = FileUtil.readBytes(file);
			String f = MD5Util.getFileMD5String(file);
			if (f.equals(fileMd5)) {
				return true;
			}
		}
		return false;
	}

	// public boolean sourceExist(String originalFilename, byte[] bytes) {
	// return false;
	// }

	public String timeStampFilename(String filename) {
		long currentTime = System.currentTimeMillis();
		if (filename.indexOf(".") != -1) {
			String[] filenameArr = filename.split("\\.");
			return filenameArr[0] + "_" + currentTime + "." + filenameArr[1];
		}
		return filename + "_" + System.currentTimeMillis();
	}

	public String getNoTimeStampFilename(String filename) {
		if (filename.indexOf(".") != -1) {
			String[] filenameArr = filename.split("\\.");
			String[] arr = filenameArr[0].split("_");
			return arr[0] + "." + filenameArr[1];
		}
		String[] arr = filename.split("_");
		return arr[0];
	}

	public File loadSource(String url) throws IOException, InterruptedException {
		url = "http://doc.tdenergys.com/doc/587fa6929c46b58bdc628278.jar";
		HttpHeaders headers = new HttpHeaders();
		// headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<byte[]>(headers), byte[].class);
		// ResponseEntity<byte[]> response = export(url);
		File file = new File("source/587fa6929c46b58bdc628278.jar");
		FileUtils.writeByteArrayToFile(file, response.getBody());
		// ready(file);
		return file;
		// exec的必须是可执行的程序，如果是命令行的命令则还需另外处理
		// 2. 在windows中process = runtime.exec(new String[] { "cmd.exe","/C",
		// "dir"});
		// 3. 在linux中process = runtime.exec(new String[] { "/bin/sh","-c", "echo
		// $PATH"});
	}

	public String releaseStart(String sourceInfoName) throws IOException, ServiceException {
		logger.info("releaseStart {}", sourceInfoName);
		File sourceInfo = new File(FileNameUtil.concat(actuatorConfig.getSourcePath(), sourceInfoName + ".info"));
		String sourcePath = FileUtils.readFileToString(sourceInfo);
		File sourceFile = new File(sourcePath);
		// File releaseDir = new File(Constant.RELEASE_ROOT +
		// FileNameUtil.getBaseName(sourceFile.getName()));
		String filename = getNoTimeStampFilename(sourceFile.getName());

		String folderPath = StringUtil.join(new Object[] { actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), "bak" }, "/");
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdir();
		}

		String jarFolderPath = StringUtil.join(new Object[] { actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename) }, "/");
		File jarFolder = new File(jarFolderPath);
		if (jarFolder.exists() && jarFolder.isDirectory()) {
			for (File file : jarFolder.listFiles()) {
				if (file.getName().indexOf("bak") != -1) {
					continue;
				}
				if (file.isDirectory()) {
					programServiceImpl.killProgram(file.getPath());
					FileUtils.copyDirectoryToDirectory(file, folder);
					FileUtils.deleteDirectory(file);
				}
			}
		}

		String releasePath = StringUtil.join(new Object[] { actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), System.currentTimeMillis(), filename }, "/");
		File releaseFile = new File(FileNameUtil.normalize(releasePath, true));
		if (!FileUtils.exists(releaseFile)) {
			FileUtils.copyFile(sourceFile, releaseFile, true);
		} else {

		}
		// programServiceImpl.structure(releaseFile);
		programServiceImpl.startProgram(releaseFile.getParent());
		return releaseFile.getPath();
	}

	public FileInfo getReleaseRootTree(String path, String sourceInfoName) throws IOException, ServiceException {
		String rootPath = this.getReleaseRootPath(path, sourceInfoName);
		FileInfo rootFile = folderFileServiceImpl.getFileByPath(rootPath);
		List<FileInfo> fileList = folderFileServiceImpl.getFolderList(rootPath, "bak");
		rootFile.setChildren(fileList);
		rootFile.setRootFolder(true);
		return rootFile;
	}

	public String getReleaseRootPath(String path, String sourceInfoName) throws IOException, ServiceException {
		File sourceInfo = new File(FileNameUtil.concat(path, sourceInfoName + ".info"));
		String sourcePath = FileUtils.readFileToString(sourceInfo);
		File sourceFile = new File(sourcePath);
		String filename = getNoTimeStampFilename(sourceFile.getName());
		String jarFolderPath = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename));
		logger.info("release rootpath {}", jarFolderPath);
		return jarFolderPath;
	}

	private String concatPath(Object... path) {
		return FileNameUtil.normalize(StringUtil.join(path, "/"), true);
	}

	public synchronized String releaseDesignatedDirStart(String path, String sourceInfoName, String dirname, boolean guard) throws ServiceException {
		try {
			if (StringUtils.isBlank(dirname)) {
				dirname = "default";
			}
			File sourceInfo = new File(FileNameUtil.concat(path, sourceInfoName + ".info"));
			String sourcePath = FileUtils.readFileToString(sourceInfo);
			File sourceFile = new File(sourcePath);
			String filename = getNoTimeStampFilename(sourceFile.getName());
			// 备份
			String releaseFolder = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), dirname);
			// String releasePath = concatPath(releaseFolder, filename);
			if (programServiceImpl.existProgram(releaseFolder)) {
				programServiceImpl.killProgram(releaseFolder);
			}
			releaseDesignatedDir(path, sourceInfoName, dirname);
			programServiceImpl.startProgram(releaseFolder);
			return releaseFolder;
		} catch (Exception e) {
			logger.error("发布程序失败", e);
			throw new ServiceException(e.getMessage(), e);
		}
	}

	/**
	 * 发布到指定的文件夹
	 * 
	 * @param sourceInfoName
	 * @param dirname
	 * @return
	 * @throws IOException
	 * @throws ServiceException
	 */
	public String releaseDesignatedDir(String path, String sourceInfoName, String dirname) throws ServiceException {
		if (StringUtils.isBlank(dirname)) {
			dirname = "default";
		}
		logger.info("release {}", sourceInfoName);
		logger.info("release dirname {}", dirname);
		try {
			File sourceInfo = new File(FileNameUtil.concat(path, sourceInfoName + ".info"));
			String sourcePath = FileUtils.readFileToString(sourceInfo);
			File sourceFile = new File(sourcePath);
			String filename = getNoTimeStampFilename(sourceFile.getName());
			// 备份
			String releaseFolder = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), dirname);
			String releasePath = concatPath(releaseFolder, filename);
			programServiceImpl.toBuckup(releaseFolder);
			File releaseFile = new File(releasePath);
			FileUtils.copyFile(sourceFile, releaseFile, true);
			File configPath = new File(concatPath(releaseFolder, "config"));
			if (!FileUtil.isExistingFolder(configPath)) {
				ZipUtil.unzip(releaseFile, configPath, "BOOT-INF/classes/application.properties");
				File oldApplication = new File(concatPath(releaseFolder, "config/BOOT-INF/classes/application.properties"));
				File newApplication = new File(concatPath(releaseFolder, "config/application.properties"));
				if (FileUtil.isExistingFile(oldApplication)) {
					FileUtil.move(oldApplication, newApplication);
					FileUtil.delete(new File(concatPath(releaseFolder, "config/BOOT-INF/")));
				}
			}
			programServiceImpl.scanningProgramList();
			return releaseFolder;
		} catch (Exception e) {
			throw new ServiceException(e.getMessage(), e);
		}
	}

	public File getJarFileByDirectory(String path) throws IOException {
		File file = FileUtils.getFile(path);
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				if (f.getName().endsWith(".jar")) {
					return f;
				}
			}
		}
		return null;
	}

	public String release(String sourceInfoName) throws IOException, ServiceException {
		logger.info("release {}", sourceInfoName);
		File sourceInfo = new File(FileNameUtil.concat(actuatorConfig.getSourcePath(), sourceInfoName + ".info"));
		String sourcePath = FileUtils.readFileToString(sourceInfo);
		File sourceFile = new File(sourcePath);
		// File releaseDir = new File(Constant.RELEASE_ROOT +
		// FileNameUtil.getBaseName(sourceFile.getName()));
		String filename = getNoTimeStampFilename(sourceFile.getName());

		String folderPath = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), "bak");
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdir();
		}

		String jarFolderPath = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename));
		File jarFolder = new File(jarFolderPath);
		if (jarFolder.exists() && jarFolder.isDirectory()) {
			for (File file : jarFolder.listFiles()) {
				if (file.getName().indexOf("bak") != -1) {
					continue;
				}
				if (file.isDirectory()) {
					programServiceImpl.killProgram(file.getPath());
					FileUtils.copyDirectoryToDirectory(file, folder);
					FileUtils.deleteDirectory(file);
				}
			}
		}

		String releasePath = concatPath(actuatorConfig.getReleasePath(), FileNameUtil.getBaseName(filename), System.currentTimeMillis(), filename);
		File releaseFile = new File(FileNameUtil.normalize(releasePath, true));
		if (!FileUtils.exists(releaseFile)) {
			// FileUtils.copyFileToDirectory(sourceFile,
			// releaseFile.getParentFile(), true);
			FileUtils.copyFile(sourceFile, releaseFile, true);
		} else {

		}
		// programServiceImpl.structure(releaseFile);
		// try {
		// Thread.sleep(3000l);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// programServiceImpl.startProgram(releaseFile.getParent());
		return releaseFile.getPath();
		// programServiceImpl.startProgram(releaseFile.getPath());
		// exec("cmd /c cd " + releaseFile.getParentFile() + " && start.bat");
	}

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
		File[] files = recoveryDir.listFiles();
		// 是否替换
		if (!replace) {
			File[] temp = FileUtils.filesMatchingStemRegex(recoveryDir, ".*.info");
			if (temp.length == 0) {
				throw new ServiceException(".info 文件不存在");
			}
			File tempSource = new File(FileNameUtil.concat(sourceDir.getPath(), temp[0].getName()));
			if (FileUtils.exists(tempSource.getPath())) {
				throw new ServiceException("此文件在资源目录中已存在");
			}
		}
		String infoName = null;
		String fileName = null;

		for (File f : files) {
			if (f.getName().endsWith(".recovery")) {
				continue;
			}
			if (f.getName().endsWith(".info")) {
				infoName = f.getName();
			} else {
				fileName = f.getName();
			}
			FileUtils.moveFileToDirectory(f, sourceDir, false);
		}

		// 如果还原的是最新文件
		File lastFile = new File(FileNameUtil.concat(sourcePath, "source.last"));

		if (!lastFile.exists()) {
			saveLast(sourcePath, FileNameUtil.concat(sourcePath, fileName), FileNameUtil.getBaseName(infoName));
		}

		List<String> lastInfo = FileUtils.readLines(lastFile);
		if (lastInfo != null && lastInfo.size() > 0) {
			String lastPath = lastInfo.get(lastInfo.size() - 2);
			File lastf = new File(lastPath);
			File reductionFile = new File(FileNameUtil.concat(sourcePath, fileName));
			if (lastf.lastModified() < reductionFile.lastModified()) {
				List<String> reduLast = ListUtils.newArrayList(reductionFile.getPath(), FileNameUtil.getBaseName(infoName));
				FileUtils.writeLines(lastFile, reduLast, true);
			}
		} else if (lastInfo.size() == 0) {
			File reductionFile = new File(FileNameUtil.concat(sourcePath, fileName));
			List<String> reduLast = ListUtils.newArrayList(reductionFile.getPath(), FileNameUtil.getBaseName(infoName));
			FileUtils.writeLines(lastFile, reduLast, true);
		}
		//
		FileUtils.deleteDirectory(recoveryDir);
	}

	public void remove(String path, String sourceInfoName) throws IOException {
		logger.info("remove {}", sourceInfoName);
		// 删除一个source 需要将.last 中的文件更新
		File sourceInfo = new File(FileNameUtil.concat(path, sourceInfoName + ".info"));
		String sourcePath = FileUtils.readFileToString(sourceInfo);
		File sourceFile = new File(sourcePath);
		// FileUtils.deleteQuietly(sourceFile);
		// FileUtils.deleteQuietly(sourceInfo);
		// 获取.last文件
		File last = new File(FileNameUtil.concat(path, "source.last"));
		if (last.exists()) {
			List<String> lastInfo = FileUtils.readLines(last);
			// 如果删除的是最后的一个版本
			if (sourceInfoName.equals(lastInfo.get(lastInfo.size() - 1))) {
				int length = lastInfo.size();
				lastInfo.remove(length - 1);
				lastInfo.remove(length - 2);
				if (lastInfo.size() > 0) {
					FileUtils.writeLines(last, lastInfo, false);
				} else {
					FileUtils.deleteQuietly(last);
				}
			}
		}
		// 移动jar文件和info文件到 recovery/source
		String timeTag = System.currentTimeMillis() + "";
		File recoverySource = new File(actuatorConfig.getSourceRecoveryPath() + timeTag);
		FileUtils.moveFileToDirectory(sourceInfo, recoverySource, true);
		FileUtils.moveFileToDirectory(sourceFile, recoverySource, false);
		// 创建.recovery文件
		File recoveryfile = new File(FileNameUtil.concat(recoverySource.getPath(), timeTag + ".recovery"));
		Map<String, Object> map = MapUtils.newHashMap();
		map.put("filename", sourceFile.getName());
		map.put("time", new Date());
		map.put("dir", path);
		map.put("sourceName", sourceFile.getName());
		map.put("type", "source");
		FileUtils.writeStringToFile(recoveryfile, JSON.toJSONString(map));
	}

	public DataPaging<SourceInfo> searchSourceList(String mainFileName, String path, String extension) {
		DataPaging<SourceInfo> page = this.findSourceList(path, extension);
		if (StringUtils.isBlank(mainFileName)) {
			return page;
		}
		List<SourceInfo> list = page.getList();
		List<SourceInfo> resultList = ListUtils.newArrayList();
		boolean flag = false;
		for (SourceInfo info : list) {
			flag = info.getMianFile().indexOf(mainFileName) != -1;
			if (flag) {
				resultList.add(info);
			}
			flag = false;
		}
		DataPaging<SourceInfo> resultPage = new DataPaging<SourceInfo>(resultList, resultList.size());
		return resultPage;
	}

	public DataPaging<SourceInfo> findSourceList(String path, String extension) {
		File[] files = FileUtils.filesMatchingStemRegex(new File(StringUtils.isBlank(path) ? actuatorConfig.getSourcePath() : path), ".*." + extension);
		List<SourceInfo> SourceInfos = toSourceInfo(Lists.newArrayList(files));
		Collections.sort(SourceInfos, new Comparator<SourceInfo>() {
			@Override
			public int compare(SourceInfo o1, SourceInfo o2) {
				return o1.getCreateTime().after(o2.getCreateTime()) ? -1 : 1;
			}
		});
		DataPaging<SourceInfo> dataPaging = new DataPaging<SourceInfo>(SourceInfos, SourceInfos.size());
		return dataPaging;
	}

	// public static void main(String[] args) {
	// File file = new File("C:\\Users\\chen\\Desktop\\phptest");
	//
	// System.out.println(file.exists());
	// System.out.println(file.isDirectory());
	// File[] files = FileUtils.filesMatchingStemRegex(file, ".*.info");
	// System.out.println(files);
	// }

	public SourceInfo toSourceInfo(File file) {
		SourceInfo info = new SourceInfo();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(file);
			File jarFile = new File(lines.get(lines.size() > 2 ? lines.size() - 2 : 0));
			info.setMianFile(jarFile.getName());
			info.setFolder(FileNameUtil.normalize(jarFile.getParentFile().getPath(), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (FilenameUtils.getExtension(file.getName()).equals("last")) {
			String infoName = lines.get(lines.size() - 1);
			info.setSourceInfoFile(infoName);
		} else {
			info.setSourceInfoFile(FileNameUtil.getBaseName(file.getName()));
		}
		info.setCreateTime(new Date(file.lastModified()));
		info.setStauts(false);
		return info;
	}

	public List<SourceInfo> toSourceInfo(List<File> files) {
		List<SourceInfo> list = new ArrayList<SourceInfo>();
		for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
			File file = iterator.next();
			list.add(toSourceInfo(file));
		}
		return list;
	}

}

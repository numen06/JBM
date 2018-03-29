/**
 * 
 */
package com.td.framework.devops.actuator.service.impl;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.td.framework.devops.actuator.bean.FileInfo;
import com.td.framework.devops.actuator.service.ProcessService;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.util.FileUtils;
import com.td.util.ListUtils;
import com.td.util.StringUtils;

import jodd.io.FileNameUtil;

/**
 * @author Leonid
 *
 */
@Service
public class FolderFileServiceImpl {

	private final static Logger logger = LoggerFactory.getLogger(FolderFileServiceImpl.class);

	@Autowired
	private ProcessService processService;

	@Value("${file.edit.type}")
	private String editType;

	public static String[] EDIT_TYPE = null;

	@PostConstruct
	private void init() {
		EDIT_TYPE = editType.split(",");
	}

	public String readFiletoString(String filepath) throws IOException {
		File file = new File(filepath);
		return FileUtils.readFileToString(file);
	}

	public void writeStringtoFile(String filepath, String str) throws IOException {
		File file = new File(filepath);
		FileUtils.writeStringToFile(file, str, false);
	}

	public boolean makeDir(String filepath, String dirname) throws IOException {
		logger.info("filepath {}", filepath);
		logger.info("dirname {}", dirname);
		File file = new File(FileNameUtil.concat(filepath, dirname, true));
		return file.mkdir();
	}

	public String renameFile(String oldPath, String newName) throws IOException, ServiceException {
		logger.info("oldpath {}", oldPath);
		logger.info("newName {}", newName);
		File oldFile = new File(oldPath);
		File[] files = FileUtils.filesMatchingStemRegex(oldFile, ".*.pid");
		if (files == null || files.length == 0) {
			throw new ServiceException("未找到pid文件");
		}
		Integer pid = Integer.parseInt(FileUtils.readFileToString(files[0]));
		if (processService.exists(pid)) {
			throw new IOException("请关闭程序后修改名称");
		}

		String path = FileNameUtil.getPath(oldPath) + newName;
		boolean res = oldFile.renameTo(new File(path));
		if (res) {
			return path;
		} else {
			throw new IOException("请关闭程序后修改名称");
		}
	}

	public byte[] downloadFile(String filepath) throws IOException {
		logger.info("downloadFile {}", filepath);
		if (StringUtils.isBlank(filepath)) {
			throw new IllegalArgumentException();
		}
		File file = FileUtils.getFile(filepath);
		if (file.isDirectory()) {
			throw new IOException(MessageFormat.format("{0} is directory", filepath));
		}
		return FileUtils.readFileToByteArray(file);
	}

	public void uploadFile(String folderPath, String filename, byte[] resource) throws IOException {
		logger.info("uploadFile {}", FileNameUtil.concat(folderPath, filename, true));
		File file = new File(FileNameUtil.concat(folderPath, filename, true));
		FileUtils.writeByteArrayToFile(file, resource);
	}

	public void deleteFile(String filepath) throws IOException {
		logger.info("deleteFile {}", filepath);
		File file = FileUtils.getFile(filepath);
		FileUtils.deleteQuietly(file);
	}

	public FileInfo getTreeFolder(String path, String filter) throws IOException {
		FileInfo info = this.getFileByPath(path);
		if (!info.isFolder()) {
			return null;
		} else {
			List<FileInfo> children = this.getFileList(path);
			List<FileInfo> list = ListUtils.newArrayList();
			for (FileInfo i : children) {
				i = this.getTreeFolder(i.getPath(), filter);
				if (i != null && i.isFolder() && i.getName().indexOf(filter) == -1) {
					list.add(i);
				}
			}
			info.setChildren(list);
		}
		return info;
	}

	public FileInfo getTreeFile(String path) throws IOException {
		FileInfo info = this.getFileByPath(path);
		if (info.isFolder()) {
			List<FileInfo> children = this.getFileList(path);
			List<FileInfo> list = ListUtils.newArrayList();
			for (FileInfo i : children) {
				i = this.getTreeFile(i.getPath());
				list.add(i);
			}
			info.setChildren(list);
		}
		return info;
	}

	public FileInfo getFileByPath(String path) throws IOException {
		if (StringUtils.isBlank(path)) {
			throw new IOException("folder path is error");
		}
		File file = FileUtils.getFile(path);
		FileInfo fileInfo = convertFileInfo(file);
		return fileInfo;
	}

	public List<FileInfo> getFolderList(String folderPath, String filter) throws IOException {
		if (StringUtils.isBlank(folderPath)) {
			throw new IOException("folder path is error");
		}
		File file = FileUtils.getFile(folderPath);
		if (!file.exists()) {
			return null;
		}
		if (!file.isDirectory()) {
			throw new IOException(MessageFormat.format("{0} is not directory", folderPath));
		}
		File[] files = file.listFiles();
		List<FileInfo> list = ListUtils.newArrayList();
		FileInfo info = null;
		for (File f : files) {
			if (f.isDirectory() && f.getName().indexOf(filter) == -1) {
				info = convertFileInfo(f);
				list.add(info);
			}
		}
		return list;
	}

	public List<FileInfo> getFileList(String folderPath) throws IOException {
		if (StringUtils.isBlank(folderPath)) {
			throw new IOException("folder path is error");
		}
		File file = FileUtils.getFile(folderPath);
		if (!file.isDirectory()) {
			throw new IOException(MessageFormat.format("{0} is not directory", folderPath));
		}
		File[] files = file.listFiles();
		List<FileInfo> list = ListUtils.newArrayList();
		FileInfo info = null;
		for (File f : files) {
			info = convertFileInfo(f);
			list.add(info);
		}
		return list;
	}

	public FileInfo convertFileInfo(File f) {
		FileInfo info = null;
		info = new FileInfo();
		info.setId(Long.parseLong(info.hashCode() + ""));
		info.setName(f.getName());
		info.setEdit(ArrayUtils.contains(EDIT_TYPE, FilenameUtils.getExtension(f.getName())));
		info.setPath(f.getPath());
		info.setUpdateTime(f.lastModified());
		info.setFolder(f.isDirectory());
		info.setSize(f.length());
		info.setRootFolder(false);
		return info;
	}
}

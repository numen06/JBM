package com.td.framework.devops.client.service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.td.util.FileUtils;

import jodd.io.FileNameUtil;
import jodd.util.StringUtil;

@Service
public class LocalFileService {

	private final static Logger logger = LoggerFactory.getLogger(LocalFileService.class);

	public File[] findFiles(String path, String name) {
		File[] files = FileUtils.filesMatchingStemRegex(new File(path), name);
		logger.info(StringUtil.join(files, ";"));
		return files;
	}
	
	public File findFile(String path, String name) {
		File file = FileUtils.getFile(FileNameUtil.concat(path, name));
		System.out.println(file.getPath());
		return file;
	}
	
	// public static void main(String[] args) {
	// File file = new
	// File("D:/maven_workspaces/td-framework/td-framework-devops/td-framework-devops-client");
	// File[] files = FileUtils.filesMatchingStemRegex(file,
	// "/target/td-framework-devops-client.*");
	// System.out.println(StringUtil.join(files, "\r\n"));
	// }
}

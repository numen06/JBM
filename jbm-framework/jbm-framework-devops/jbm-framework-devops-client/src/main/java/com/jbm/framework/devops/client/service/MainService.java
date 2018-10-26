package com.jbm.framework.devops.client.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jbm.devops.bean.ReleaseOption;
import com.jbm.framework.devops.client.bean.ConfigOption;
import com.jbm.framework.metadata.usage.bean.FileInfoBean;
import com.jbm.util.CollectionUtils;
import com.jbm.util.StringUtils;

@Service
public class MainService {

	private static Logger logger = LoggerFactory.getLogger(MainService.class);

	private ConfigOption config = ConfigOption.getInstance();

	@Autowired
	private LocalFileService localFileService;

	@Autowired
	private SendFileService sendFileService;

	@Autowired
	private RequestActuatorService requestActuatorService;

	@PostConstruct
	public void init() {
		logger.info("path {}", config.getPath());
		logger.info("name {}", config.getName());
		logger.info("release {}", config.getRelease());
		logger.info("start {}", config.getStart());
		logger.info("url {}", config.getUrl());
		this.publishSource();
	}

	public void publishSource() {
		// 找到文件
		ReleaseOption option = new ReleaseOption();
		option.setRelease(config.getRelease());
		option.setStart(config.getStart());
		if (StringUtils.isNotBlank(config.getDoc())) {
			remoteSource(option);
		} else {
			localSource(option);
		}
		if (CollectionUtils.isEmpty(option.getFileInfos())) {
			logger.error("源码包为空，无法发布");
			return;
		}
		requestActuatorService.publishSource(option);
	}

	public void localSource(ReleaseOption option) {
		File file = localFileService.findFile(config.getPath(), config.getName());
		// 上传文件
		List<FileInfoBean> fileInfos = sendFileService.sendFile(file);
		option.setFileInfos(fileInfos);
	}

	public void remoteSource(ReleaseOption option) {
		List<FileInfoBean> fileInfos = new ArrayList<>();
		FileInfoBean fileBean = new FileInfoBean();
		fileBean.setId(config.getDoc());
		fileBean.setFileName(config.getName());
		fileInfos.add(fileBean);
		option.setFileInfos(fileInfos);
	}

}

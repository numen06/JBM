package com.td.framework.devops.client.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.td.framework.metadata.usage.bean.FileInfoBean;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

@Service
public class SendFileService {
	private final static Logger logger = LoggerFactory.getLogger(SendFileService.class);

	// private RealRestTemplate realRestTemplate =
	// RestTemplateFactory.getInstance().createRealRestTemplate();

	@Value("${doc.url}")
	private String docUrl;

	public List<FileInfoBean> sendFile(File file) {
		return sendFile(new File[] { file });
	}

	public List<FileInfoBean> sendFile(File[] files) {
		List<FileInfoBean> fileInfos = new ArrayList<FileInfoBean>();
		if (ArrayUtils.isEmpty(files))
			return fileInfos;
		logger.info("docUrl {}", docUrl);
		try {
			HttpRequest httpRequest = HttpRequest.post(docUrl);
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				httpRequest.form("file[" + i + "]", file).form("filename", file.getName());
			}
			HttpResponse httpResponse = httpRequest.send();
			if (StringUtils.isNotBlank(httpResponse.bodyText())) {
//				logger.info("result {}", httpResponse.bodyText());
				String result = JSON.parseObject(httpResponse.body(), JSONObject.class).getString("result");
				fileInfos = JSON.parseArray(result, FileInfoBean.class);
			}
			// String fileResponse = JSON.parseObject(httpResponse.body(),
			// HashMap.class).get("result").toString();
		} catch (Exception e) {
			logger.error("上传代码失败", e);
		}
		return fileInfos;
	}

}

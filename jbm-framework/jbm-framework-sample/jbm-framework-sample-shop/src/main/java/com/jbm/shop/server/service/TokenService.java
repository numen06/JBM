package com.jbm.shop.server.service;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.jbm.shop.server.bean.WeiDianResuleBean;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import jodd.io.FileUtil;

@Service
public class TokenService {

	private final static Logger logger = LoggerFactory.getLogger(TokenService.class);

	private String grant_type = "client_credential";

	private String appkey = "673750";

	private String secret = "0690adc24e3025650861be2385444ffe";

	private String access_token;
	private File access_token_file = new File("access_token.txt");

	@PostConstruct
	public void init() throws IOException {
		if (FileUtil.isExistingFile(access_token_file)) {
			this.access_token = FileUtil.readString(access_token_file);
		} else {
			getToken();
			FileUtil.writeString(access_token_file, access_token);
		}
		logger.info("access_token:{}", access_token);

	}

	public String getToken() {
		if (access_token != null)
			return access_token;
		HttpRequest httpRequest = HttpRequest.get("https://api.vdian.com/token").query("grant_type", grant_type).query("appkey", appkey).query("secret", secret);
		HttpResponse response = httpRequest.send();
		WeiDianResuleBean body = JSON.parseObject(response.body(), WeiDianResuleBean.class);
		this.access_token = body.getResult().get("access_token").toString();
		return access_token;
	}

}

package com.jbm.ai;

import java.io.Serializable;

import org.springframework.data.annotation.Transient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.turing.util.Aes;
import com.turing.util.Md5;

public class RobotRequestBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6008240422178666834L;
	@Transient
	private RobotUserInfo userInfo = new RobotUserInfo();
	@Transient
	private final Aes aes;
	@Transient
	private String timestamp = String.valueOf(System.currentTimeMillis());
	private String info;
	private String loc = "上海";
	private String userid = "12345577";

	public RobotRequestBean() {
		super();
		// 生成密钥
		String keyParam = userInfo.getSecret() + timestamp + userInfo.getApiKey();
		String md5key = Md5.MD5(keyParam);
		// 加密
		this.aes = new Aes(md5key);
	}

	public String getKey() {
		return userInfo.getApiKey();
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getLoc() {
		return loc;
	}

	public void setLoc(String loc) {
		this.loc = loc;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String encrypt() {
		String data = aes.encrypt(JSON.toJSONString(this));
		JSONObject json = new JSONObject();
		json.put("key", userInfo.getApiKey());
		json.put("timestamp", this.timestamp);
		json.put("data", data);
		// System.out.println(data);
		return JSON.toJSONString(json);
	}

}

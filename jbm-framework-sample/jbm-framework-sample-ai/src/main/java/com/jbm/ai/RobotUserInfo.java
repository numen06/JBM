package com.jbm.ai;

public class RobotUserInfo {
	private final String apiKey = "cf8ccaf41b864f1d8b35ae52f089a365";
	private final String secret = "ae55707291ddfd7b";
	private String userId = "wesley";

	public String getApiKey() {
		return apiKey;
	}

	public String getSecret() {
		return secret;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}

package com.td.test;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.td.framework.metadata.exceptions.http.HttpConnectionException;
import com.td.framework.metadata.exceptions.http.HttpRequestException;
import com.td.framework.tools.EasyHttpClient;
import com.td.framework.utils.HttpServiceHelper;

public class HttpClientTest {

	private static EasyHttpClient _httpClient;

	static {
		String authCode = HttpServiceHelper.getBasicAuthorization("1", "123456");
		_httpClient = new EasyHttpClient(authCode, 30, null);
	}

	@Test
	public void testPost() {
		// String https_url =
		// "https://127.0.0.1:8443/https-test/push/sendMessage.do";
		JSONObject obj = new JSONObject();
		obj.put("str", "逆变器1");
		try {
			System.out.println(_httpClient.sendPost("https://127.0.0.1:8443/https-test/push/testForm.do", obj.toJSONString()).responseContent);
			System.out.println(_httpClient.sendFormPost("https://127.0.0.1:8443/https-test/push/testJson.do", obj.toJSONString()).responseContent);
		} catch (HttpConnectionException e) {
			e.printStackTrace();
		} catch (HttpRequestException e) {
			e.printStackTrace();
		}
	}
}

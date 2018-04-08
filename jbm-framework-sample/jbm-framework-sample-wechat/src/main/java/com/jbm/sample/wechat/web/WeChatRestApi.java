package com.jbm.sample.wechat.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;
import org.sword.wechat4j.jsapi.JsApiManager;
import org.sword.wechat4j.jsapi.JsApiParam;
import org.sword.wechat4j.message.CustomerMsg;
import org.sword.wechat4j.oauth.OAuthException;
import org.sword.wechat4j.oauth.OAuthManager;
import org.sword.wechat4j.oauth.protocol.get_access_token.GetAccessTokenRequest;
import org.sword.wechat4j.oauth.protocol.get_access_token.GetAccessTokenResponse;
import org.sword.wechat4j.pay.PayManager;
import org.sword.wechat4j.pay.exception.PayApiException;
import org.sword.wechat4j.pay.exception.PayBusinessException;
import org.sword.wechat4j.pay.exception.SignatureException;
import org.sword.wechat4j.pay.protocol.unifiedorder.UnifiedorderRequest;
import org.sword.wechat4j.user.LanguageType;
import org.sword.wechat4j.user.User;
import org.sword.wechat4j.user.UserManager;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.sample.wechat.service.MyWechat;

@RestController
@RequestMapping("/api")
public class WeChatRestApi {

	private static Logger logger = Logger.getLogger(WeChatRestApi.class);

	private JsApiParam jsApiParam;

	@RequestMapping("")
	public void wechat(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		MyWechat lejian = new MyWechat(request);
		String result = lejian.execute();
		response.getOutputStream().write(result.getBytes());
		CustomerMsg customerMsg = new CustomerMsg("o9CFtv4fWkOBrpSzFs5zDQLx06-w");
		customerMsg.sendText(lejian.getWechatRequest().getContent());
	}

	@RequestMapping("/jsapi")
	public Object jsapi(String url) throws ServletException, IOException, OAuthException {
		logger.info("js请求授权地址：" + url);
		JsApiParam jsApiParam = JsApiManager.signature(url);
		return jsApiParam;
	}

	@RequestMapping("/auth2")
	public String auth2(String code, String state) throws ServletException, IOException, OAuthException {
		logger.info("微信授权成功" + code + "," + state);
		GetAccessTokenResponse getAccessTokenResponse = OAuthManager.getAccessToken(new GetAccessTokenRequest(code));
		User user = new UserManager().getUserInfo(getAccessTokenResponse.getOpenid(), LanguageType.zh_CN);
		CustomerMsg customerMsg = new CustomerMsg(getAccessTokenResponse.getOpenid());
		customerMsg.sendText(MessageFormat.format("亲爱的：{0},您的微信授权成功", HtmlUtils.htmlUnescape(user.getNickName())));
		System.out.println(getAccessTokenResponse);
		return "微信授权成功";
	}

	@RequestMapping("/payAuth")
	public Object payAuth(String timeStamp, String nonceStr, String prepayId) throws ServletException, IOException, OAuthException {
		return PayManager.buildH5PayConfig(timeStamp, nonceStr, prepayId);
	}

	@RequestMapping("/getAccessToken")
	public Object getAccessToken(String code) throws ServletException, IOException, OAuthException, SignatureException, PayApiException, PayBusinessException {
		GetAccessTokenRequest request = new GetAccessTokenRequest(code);

		return OAuthManager.getAccessToken(request);
	}

	@RequestMapping("/unifiedOrder")
	public Object unifiedOrder(@RequestBody(required = false) String requestBody) {
		UnifiedorderRequest request = JSON.parseObject(requestBody, UnifiedorderRequest.class);
		try {
			return PayManager.unifiedorder(request);
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(e, "unifiedOrder error");
		}
	}

	@RequestMapping("/generateRedirectURI")
	public Object generateRedirectURI(String redirectURI, String scope, String state)
		throws ServletException, IOException, OAuthException, SignatureException, PayApiException, PayBusinessException {
		return OAuthManager.generateRedirectURI(redirectURI, scope, state);
	}

	@RequestMapping("/pay")
	public String pay(String code, String state) throws ServletException, IOException, OAuthException {
		logger.info("微信授权成功" + code + "," + state);
		GetAccessTokenResponse getAccessTokenResponse = OAuthManager.getAccessToken(new GetAccessTokenRequest(code));
		CustomerMsg customerMsg = new CustomerMsg(getAccessTokenResponse.getOpenid());
		customerMsg.sendText("微信授权成功");
		System.out.println(getAccessTokenResponse);
		return "微信授权成功";
	}

}

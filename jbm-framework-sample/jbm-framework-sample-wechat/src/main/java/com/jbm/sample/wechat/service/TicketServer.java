package com.jbm.sample.wechat.service;

import org.sword.wechat4j.token.server.JsApiTicketServer;

public class TicketServer extends JsApiTicketServer {

	@Override
	public String ticket() {
		return "http://www.tdenergys.com/mwechat";
	}

}

package com.td.framework.devops.actuator.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.client.ReconnConf;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.Node;

import td.framework.boot.autoconfigure.tio.AioClientTemplate;
import td.framework.boot.autoconfigure.tio.handler.JsonClientAioHandler;

@Configuration
public class DevopsServerConfig {
	@Bean("devopsClientTemplate")
	public AioClientTemplate aioClientTemplate() {
		// 断链后自动连接的，不想自动连接请设为null
		ReconnConf reconnConf = new ReconnConf(5000L);
		// handler, 包括编码、解码、消息处理
		ClientAioHandler aioClientHandler = new JsonClientAioHandler();
		Node serverNode = new Node("127.0.0.1", 6789);
		return new AioClientTemplate(aioClientHandler, null, serverNode, reconnConf);
	}

}

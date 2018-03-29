package com.td.test.tio;

import java.util.TimerTask;
import java.util.UUID;

import org.tio.client.AioClient;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.client.intf.ClientAioHandler;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.Node;

import com.td.util.ScheduleUtils;

import td.framework.boot.autoconfigure.tio.handler.JsonClientAioHandler;
import td.framework.boot.autoconfigure.tio.packet.JsonPacket;

public class JsonClientStarter {
	// 服务器节点
	public static Node serverNode = new Node(Const.SERVER, Const.PORT);

	// handler, 包括编码、解码、消息处理
	public static ClientAioHandler aioClientHandler = new JsonClientAioHandler();

	// 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
	public static ClientAioListener aioListener = null;

	// 断链后自动连接的，不想自动连接请设为null
	private static ReconnConf reconnConf = new ReconnConf(5000L);

	// 一组连接共用的上下文对象
	public static ClientGroupContext clientGroupContext = new ClientGroupContext(aioClientHandler, aioListener, reconnConf);

	public static AioClient aioClient = null;
	public static ClientChannelContext clientChannelContext = null;

	/**
	 * 启动程序入口
	 */
	public static void main(String[] args) throws Exception {
		clientGroupContext.setHeartbeatTimeout(Const.TIMEOUT);
		aioClient = new AioClient(clientGroupContext);
		clientChannelContext = aioClient.connect(serverNode);
		// 连上后，发条消息玩玩
		ScheduleUtils.executeAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				send();
			}
		}, 0, 3000);

	}

	private static void send() {
		JsonPacket packet = new JsonPacket();
		packet.pack("hello world," + UUID.randomUUID());
		// packet.setJsonBody("hello world," + UUID.randomUUID());
		Aio.send(clientChannelContext, packet);
	}
}
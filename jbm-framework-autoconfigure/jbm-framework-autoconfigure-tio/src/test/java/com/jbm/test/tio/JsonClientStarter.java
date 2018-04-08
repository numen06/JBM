package com.jbm.test.tio;

import java.util.TimerTask;

import org.tio.client.AioClient;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.client.intf.ClientAioHandler;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.Node;

import com.jbm.util.ScheduleUtils;

import jodd.io.FileUtil;
import jbm.framework.boot.autoconfigure.tio.handler.JsonClientAioHandler;
import jbm.framework.boot.autoconfigure.tio.packet.JsonPacket;

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
		}, 0, 30000000);

	}

	private static void send() {
		try {
			JsonPacket packet = new JsonPacket();
			// packet.pack("hello world," + UUID.randomUUID());
			packet.pack(FileUtil.readBytes("D:/maven_workspaces/td-framework/td-framework-autoconfigure/td-framework-autoconfigure-tio/src/test/resources/td-dpen-tdccp.log"));
			// packet.setJsonBody("hello world," + UUID.randomUUID());
//			clientChannelContext.setPacketNeededLength(Integer.MAX_VALUE);
			Aio.send(clientChannelContext, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
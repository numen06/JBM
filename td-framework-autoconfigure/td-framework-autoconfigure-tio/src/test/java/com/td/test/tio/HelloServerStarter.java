package com.td.test.tio;

import java.io.IOException;

import org.tio.server.AioServer;
import org.tio.server.ServerGroupContext;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;

/**
 *
 * @author tanyaowu 2017年4月4日 下午12:22:58
 */
public class HelloServerStarter {
	// handler, 包括编码、解码、消息处理
	public static ServerAioHandler aioHandler = new HelloServerAioHandler();

	// 事件监听器，可以为null，但建议自己实现该接口，可以参考showcase了解些接口
	public static ServerAioListener aioListener = null;

	// 一组连接共用的上下文对象
	public static ServerGroupContext serverGroupContext = new ServerGroupContext(aioHandler, aioListener);

	// aioServer对象
	public static AioServer aioServer = new AioServer(serverGroupContext);

	// 有时候需要绑定ip，不需要则null
	public static String serverIp = null;

	// 监听的端口
	public static int serverPort = Const.PORT;

	/**
	 * 启动程序入口
	 */
	public static void main(String[] args) throws IOException {
		serverGroupContext.setHeartbeatTimeout(Const.TIMEOUT);

		aioServer.start(serverIp, serverPort);
	}
}
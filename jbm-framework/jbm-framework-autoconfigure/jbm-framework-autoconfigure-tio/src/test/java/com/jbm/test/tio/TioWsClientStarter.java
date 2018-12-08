package com.jbm.test.tio;

import java.net.URI;
import java.nio.ByteBuffer;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.alibaba.fastjson.JSON;

import jodd.io.FileUtil;
import jbm.framework.boot.autoconfigure.tio.packet.JsonForcer;

@ClientEndpoint
public class TioWsClientStarter {

	public static void main(String[] args) throws InterruptedException {
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer(); // 获取WebSocket连接器，其中具体实现可以参照websocket-api.jar的源码,Class.forName("org.apache.tomcat.websocket.WsWebSocketContainer");
			String uri = "ws://127.0.0.1:9321/mqtt/test";
			Session session = container.connectToServer(TioWsClientStarter.class, new URI(uri)); // 连接会话

			JsonForcer packet = new JsonForcer(
				FileUtil.readBytes("D:/maven_workspaces/jbm-framework/jbm-framework-autoconfigure/jbm-framework-autoconfigure-tio/src/test/resources/jbm-tdccp.log"));
			// packet.pack("hello world," + UUID.randomUUID());
			session.getBasicRemote().sendText("4564546");
			session.getBasicRemote().sendBinary(ByteBuffer.wrap(JSON.toJSONBytes(packet))); // 发送文本消息
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread.sleep(30 * 1000);
	}

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("Connected to endpoint: " + session.getBasicRemote());
	}

	@OnMessage
	public void onMessage(String message) {
		System.out.println(message);
	}

	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

}

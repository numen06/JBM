package jbm.framework.boot.autoconfigure.tio;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.server.AioServer;
import org.tio.server.ServerGroupContext;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;
import org.tio.websocket.common.WsResponse;

public class AioServerTemplate {

	private static Logger logger = LoggerFactory.getLogger(AioServerTemplate.class);
	// 一组连接共用的上下文对象
	public final ServerGroupContext serverGroupContext;
	private final Node serverNode;

	public AioServer aioServer = null;

	public AioServerTemplate(ServerAioHandler aioHandler, ServerAioListener aioListener, Node serverNode) {
		this(aioHandler, aioListener, serverNode, 5000l);
	}

	public AioServerTemplate(ServerAioHandler aioHandler, ServerAioListener aioListener, Node serverNode, long heartbeatTimeout) {
		super();
		this.serverNode = serverNode;
		this.serverGroupContext = new ServerGroupContext(aioHandler, aioListener);
	}

	public void init() throws IOException {
		this.aioServer = new AioServer(serverGroupContext);
	}

	public void start() throws Exception {
		aioServer.start(serverNode.getIp(), serverNode.getPort());
	}

	public void send(String channelId, Object obj) {
		ChannelContext channelContext = Aio.getChannelContextById(serverGroupContext, channelId);
		WsResponse response = processRetObj(obj);
		if (response == null) {
			logger.error("发送内容为空");
			return;
		}
		Aio.send(channelContext, response);
	}

	public void bSend(String channelId, Object obj) {
		ChannelContext channelContext = Aio.getChannelContextById(serverGroupContext, channelId);
		WsResponse response = processRetObj(obj);
		if (response == null) {
			logger.error("发送内容为空");
			return;
		}
		Aio.bSend(channelContext, response);
	}

	protected WsResponse processRetObj(Object obj) {
		WsResponse wsResponse = null;
		if (obj == null) {
			return null;
		} else {
			if (obj instanceof String) {
				String str = (String) obj;
				wsResponse = WsResponse.fromText(str, "utf-8");
				return wsResponse;
			} else if (obj instanceof byte[]) {
				wsResponse = WsResponse.fromBytes((byte[]) obj);
				return wsResponse;
			} else if (obj instanceof WsResponse) {
				return (WsResponse) obj;
			} else if (obj instanceof ByteBuffer) {
				byte[] bs = ((ByteBuffer) obj).array();
				wsResponse = WsResponse.fromBytes(bs);
				return wsResponse;
			} else {
				logger.error("{} ()方法，只允许返回byte[]、ByteBuffer、WsResponse或null，但是程序返回了{}", this.getClass().getName(), obj.getClass().getName());
				return null;
			}
		}
	}
}

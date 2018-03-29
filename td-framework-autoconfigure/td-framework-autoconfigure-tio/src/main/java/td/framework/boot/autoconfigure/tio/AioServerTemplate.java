package td.framework.boot.autoconfigure.tio;

import java.io.IOException;

import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.intf.Packet;
import org.tio.server.AioServer;
import org.tio.server.ServerGroupContext;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;

public class AioServerTemplate {
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

	public void send(String channelId, Packet packet) {
		ChannelContext channelContext = Aio.getChannelContextById(serverGroupContext, channelId);
		Aio.send(channelContext, packet);
	}

	public void bSend(String channelId, Packet packet) {
		ChannelContext channelContext = Aio.getChannelContextById(serverGroupContext, channelId);
		Aio.bSend(channelContext, packet);
	}
}

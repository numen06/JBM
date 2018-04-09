package jbm.framework.boot.autoconfigure.tio;

import java.io.IOException;

import org.tio.client.AioClient;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.client.intf.ClientAioHandler;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.Node;
import org.tio.core.intf.Packet;

/**
 * 
 * @author numen
 *
 */
public class AioClientTemplate {

	// 一组连接共用的上下文对象
	public final ClientGroupContext clientGroupContext;
	private final Node serverNode;

	public AioClient aioClient = null;
	public ClientChannelContext clientChannelContext;

	public AioClientTemplate(ClientAioHandler aioHandler, ClientAioListener aioListener, Node serverNode, ReconnConf reconnConf) {
		this(aioHandler, aioListener, serverNode, reconnConf, 5000l);
	}

	public AioClientTemplate(ClientAioHandler aioHandler, ClientAioListener aioListener, Node serverNode, ReconnConf reconnConf, long heartbeatTimeout) {
		super();
		this.serverNode = serverNode;
		this.clientGroupContext = new ClientGroupContext(aioHandler, aioListener, reconnConf);
	}

	public void init() throws IOException {
		this.aioClient = new AioClient(clientGroupContext);
	}

	public void connect() throws Exception {
		this.clientChannelContext = aioClient.connect(serverNode);
	}

	public void send(Packet packet) {
		Aio.send(clientChannelContext, packet);
	}

	public void bSend(Packet packet) {
		Aio.bSend(clientChannelContext, packet);
	}

}

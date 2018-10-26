package jbm.framework.boot.autoconfigure.tio;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

import org.tio.client.AioClient;
import org.tio.client.ClientChannelContext;
import org.tio.client.ClientGroupContext;
import org.tio.client.ReconnConf;
import org.tio.core.Aio;
import org.tio.core.Node;
import org.tio.http.common.HttpUuid;
import org.tio.http.server.util.Threads;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;

import jbm.framework.boot.autoconfigure.tio.handler.HttpClientAioHandler;
import jbm.framework.boot.autoconfigure.tio.handler.HttpResponseHandler;
import jbm.framework.boot.autoconfigure.tio.listener.HttpClientAioListener;
import jbm.framework.boot.autoconfigure.tio.packet.JoddHttpRequestWrap;

/**
 * @author wesley.zhang
 * @date 2018年3月28日
 * @version 1.0
 *
 */
public class AioHttpClientTemplate {

	// 一组连接共用的上下文对象
	private Node serverNode;

	public AioClient aioClient = null;
	public ClientChannelContext clientChannelContext;

	private HttpClientAioHandler httpClientAioHandler = null;

	private HttpClientAioListener httpClientAioListener = null;

	private ClientGroupContext clientGroupContext = null;

	public AioHttpClientTemplate(Node serverNode, HttpResponseHandler responseHandler) throws IOException {
		this(serverNode, responseHandler, null, null);
	}

	public AioHttpClientTemplate(Node serverNode, HttpResponseHandler responseHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
		this.serverNode = serverNode;
		if (tioExecutor == null) {
			tioExecutor = Threads.tioExecutor;
		}
		if (groupExecutor == null) {
			groupExecutor = Threads.groupExecutor;
		}
		init(responseHandler, tioExecutor, groupExecutor);
	}

	private void init(HttpResponseHandler responseHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
		this.httpClientAioHandler = new HttpClientAioHandler(responseHandler);
		httpClientAioListener = new HttpClientAioListener();
		clientGroupContext = new ClientGroupContext(httpClientAioHandler, httpClientAioListener, new ReconnConf(5));
		clientGroupContext.setHeartbeatTimeout(1000 * 20);
		// clientGroupContext.setShortConnection(true);
		// clientGroupContext.setAttribute(GroupContextKey.HTTP_SERVER_CONFIG,
		// httpConfig);
		aioClient = new AioClient(clientGroupContext);
		HttpUuid imTioUuid = new HttpUuid();
		clientGroupContext.setTioUuid(imTioUuid);
		try {
			this.connect();
		} catch (Exception e) {
			throw new IOException("连接错误");
		}
	}

	public void connect() throws Exception {
		this.clientChannelContext = aioClient.connect(serverNode);
	}

	public void send(jodd.http.HttpRequest httpRequest) {
		Aio.send(clientChannelContext, new JoddHttpRequestWrap(httpRequest));
	}

	public void bSend(jodd.http.HttpRequest httpRequest) {
		Aio.bSend(clientChannelContext, new JoddHttpRequestWrap(httpRequest));
	}
}

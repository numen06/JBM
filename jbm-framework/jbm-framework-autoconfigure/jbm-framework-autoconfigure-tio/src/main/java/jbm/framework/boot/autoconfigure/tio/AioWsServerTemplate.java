package jbm.framework.boot.autoconfigure.tio;

import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.core.intf.TioUuid;
import org.tio.http.server.util.Threads;
import org.tio.server.AioServer;
import org.tio.server.ServerGroupContext;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;
import org.tio.websocket.common.WsTioUuid;
import org.tio.websocket.server.WsServerAioHandler;
import org.tio.websocket.server.WsServerAioListener;
import org.tio.websocket.server.WsServerConfig;
import org.tio.websocket.server.handler.IWsMsgHandler;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author wesley.zhang
 * @version 1.0
 * @date 2018年3月28日
 */
public class AioWsServerTemplate {
    // private static Logger log =
    // LoggerFactory.getLogger(AioWsServerStarter.class);

    private WsServerConfig wsServerConfig = null;

    private IWsMsgHandler wsMsgHandler = null;

    private WsServerAioHandler wsServerAioHandler = null;

    private WsServerAioListener wsServerAioListener = null;

    private ServerGroupContext serverGroupContext = null;

    private AioServer aioServer = null;

    public AioWsServerTemplate(int port, IWsMsgHandler wsMsgHandler) throws IOException {
        this(port, wsMsgHandler, null, null);
    }

    public AioWsServerTemplate(int port, IWsMsgHandler wsMsgHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
        this(new WsServerConfig(port), wsMsgHandler, tioExecutor, groupExecutor);
    }

    public AioWsServerTemplate(WsServerConfig wsServerConfig, IWsMsgHandler wsMsgHandler) throws IOException {
        this(wsServerConfig, wsMsgHandler, null, null);
    }

    public AioWsServerTemplate(WsServerConfig wsServerConfig, IWsMsgHandler wsMsgHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) throws IOException {
        this(wsServerConfig, wsMsgHandler, new WsTioUuid(), tioExecutor, groupExecutor);
    }

    public AioWsServerTemplate(WsServerConfig wsServerConfig, IWsMsgHandler wsMsgHandler, TioUuid tioUuid, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor)
            throws IOException {
        if (tioExecutor == null) {
            tioExecutor = Threads.tioExecutor;
        }

        if (groupExecutor == null) {
            groupExecutor = Threads.groupExecutor;
        }
        this.wsServerConfig = wsServerConfig;
        this.wsMsgHandler = wsMsgHandler;
        wsServerAioHandler = new WsServerAioHandler(wsServerConfig, wsMsgHandler);
        wsServerAioListener = new WsServerAioListener();
        serverGroupContext = new ServerGroupContext("Tio Websocket Server", wsServerAioHandler, wsServerAioListener, tioExecutor, groupExecutor);
        serverGroupContext.setHeartbeatTimeout(0);
        serverGroupContext.setTioUuid(tioUuid);
        this.aioServer = new AioServer(serverGroupContext);
    }

    public AioWsServerTemplate(WsServerConfig wsServerConfig, IWsMsgHandler wsMsgHandler, WsServerAioListener wsServerAioListener) throws IOException {
        this.wsServerConfig = wsServerConfig;
        this.wsMsgHandler = wsMsgHandler;
        wsServerAioHandler = new WsServerAioHandler(wsServerConfig, wsMsgHandler);
        serverGroupContext = new ServerGroupContext("Tio Websocket Server", wsServerAioHandler, wsServerAioListener, Threads.tioExecutor, Threads.groupExecutor);
        serverGroupContext.setHeartbeatTimeout(0);
        serverGroupContext.setTioUuid(new WsTioUuid());
        this.aioServer = new AioServer(serverGroupContext);
    }

    /**
     * @return the wsServerConfig
     */
    public WsServerConfig getWsServerConfig() {
        return wsServerConfig;
    }

    /**
     * @return the wsMsgHandler
     */
    public IWsMsgHandler getWsMsgHandler() {
        return wsMsgHandler;
    }

    /**
     * @return the wsServerAioHandler
     */
    public WsServerAioHandler getWsServerAioHandler() {
        return wsServerAioHandler;
    }

    /**
     * @return the wsServerAioListener
     */
    public WsServerAioListener getWsServerAioListener() {
        return wsServerAioListener;
    }

    /**
     * @return the serverGroupContext
     */
    public ServerGroupContext getServerGroupContext() {
        return serverGroupContext;
    }

    public void start() throws IOException {
        aioServer.start(wsServerConfig.getBindIp(), wsServerConfig.getBindPort());
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

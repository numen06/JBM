package com.jbm.test.tio;

import jbm.framework.boot.autoconfigure.tio.AioWsServerTemplate;
import org.tio.core.ChannelContext;
import org.tio.server.ServerGroupContext;
import org.tio.websocket.server.WsServerAioListener;
import org.tio.websocket.server.WsServerConfig;

import java.io.IOException;

/**
 *
 */
public class TioWsServerStarter {

    private ServerGroupContext serverGroupContext;
    private AioWsServerTemplate wsServerStarter;
    /**
     * @author tanyaowu
     */
    public TioWsServerStarter(int port, WsMsgHandler wsMsgHandler) throws IOException {
        WsServerConfig serverConfig = new WsServerConfig(port);
        wsServerStarter = new AioWsServerTemplate(serverConfig, wsMsgHandler, new WsServerAioListener() {

            @Override
            public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) {
                super.onAfterConnected(channelContext, isConnected, isReconnect);
                System.out.println(channelContext.getId());
            }
        });
        wsServerStarter.getServerGroupContext().setHeartbeatTimeout(30 * 1000);
        serverGroupContext = wsServerStarter.getServerGroupContext();
    }

    /**
     * @param args
     * @throws IOException
     * @author tanyaowu
     */
    public static void main(String[] args) throws IOException {
        TioWsServerStarter appStarter = new TioWsServerStarter(9321, new WsMsgHandler());
        appStarter.start();
    }

    /**
     * @return the serverGroupContext
     */
    public ServerGroupContext getServerGroupContext() {
        return serverGroupContext;
    }

    public AioWsServerTemplate getWsServerStarter() {
        return wsServerStarter;
    }

    public void start() throws IOException {
        wsServerStarter.start();
    }
}
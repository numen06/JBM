package jbm.framework.boot.autoconfigure.modbus.socket.handler;

import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioListener;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-04 02:41
 **/
@Slf4j
public class MServerAioListener implements ServerAioListener {


    @Override
    public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) throws Exception {
        log.info("onAfterConnected");
    }

    @Override
    public void onAfterDecoded(ChannelContext channelContext, Packet packet, int i) throws Exception {
        log.info("onAfterDecoded");
    }

    @Override
    public void onAfterReceivedBytes(ChannelContext channelContext, int i) throws Exception {
        log.info("node:{}", channelContext.getClientNode());
    }

    @Override
    public void onAfterSent(ChannelContext channelContext, Packet packet, boolean b) throws Exception {
        log.info("onAfterSent");
    }

    @Override
    public void onAfterHandled(ChannelContext channelContext, Packet packet, long l) throws Exception {
        log.info("onAfterHandled");
    }

    @Override
    public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String s, boolean b) throws Exception {
        log.info("onBeforeClose");
    }
}

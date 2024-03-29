package jbm.framework.boot.autoconfigure.tio;

import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.server.HttpServerStarter;
import org.tio.utils.thread.pool.SynThreadPoolExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author wesley.zhang
 * @version 1.0
 * @date 2018年3月28日
 */
public class AioHttpServerTemplate extends HttpServerStarter {

    public AioHttpServerTemplate(HttpConfig httpConfig, HttpRequestHandler requestHandler) {
        super(httpConfig, requestHandler);
    }

    public AioHttpServerTemplate(HttpConfig httpConfig, HttpRequestHandler requestHandler, SynThreadPoolExecutor tioExecutor, ThreadPoolExecutor groupExecutor) {
        super(httpConfig, requestHandler, tioExecutor, groupExecutor);
    }

    public void send(String channelId, Packet packet) {
        ChannelContext channelContext = Aio.getChannelContextById(this.getServerGroupContext(), channelId);
        Aio.send(channelContext, packet);
    }

    public void bSend(String channelId, Packet packet) {
        ChannelContext channelContext = Aio.getChannelContextById(this.getServerGroupContext(), channelId);
        Aio.bSend(channelContext, packet);
    }
}

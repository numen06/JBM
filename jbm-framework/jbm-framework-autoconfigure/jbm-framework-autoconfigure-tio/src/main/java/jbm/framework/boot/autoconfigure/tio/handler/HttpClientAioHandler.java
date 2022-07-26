package jbm.framework.boot.autoconfigure.tio.handler;

import jbm.framework.boot.autoconfigure.tio.packet.JoddHttpRequestWrap;
import jbm.framework.boot.autoconfigure.tio.packet.JoddHttpResponseWrap;
import jodd.http.Cookie;
import jodd.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

public class HttpClientAioHandler implements ClientAioHandler {
    public static final String REQUEST_KEY = "tio_request_key";
    private static Logger logger = LoggerFactory.getLogger(HttpClientAioHandler.class);
    private HttpResponseHandler responseHandler;

    private Cookie[] cookies;

    public HttpClientAioHandler(HttpResponseHandler responseHandler) {
        super();
        this.responseHandler = responseHandler;
    }

    /**
     * @param buffer
     * @return
     * @throws AioDecodeException
     * @author wesley
     * @see org.tio.core.intf.AioHandler#decode(java.nio.ByteBuffer)
     */
    @Override
    public JoddHttpResponseWrap decode(ByteBuffer buffer, ChannelContext channelContext) throws AioDecodeException {
        JoddHttpResponseWrap joddHttpResponseWrap = new JoddHttpResponseWrap(channelContext);
        joddHttpResponseWrap.decode(buffer);
        this.cookies = joddHttpResponseWrap.getJodddhttpResponse().cookies();
        return joddHttpResponseWrap;
    }

    /**
     * @param packet
     * @return
     * @author wesley
     * @see org.tio.core.intf.AioHandler#encode(org.tio.core.intf.Packet)
     */
    @Override
    public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
        JoddHttpRequestWrap httpRequest = (JoddHttpRequestWrap) packet;
        if (this.cookies != null)
            httpRequest.getJoddHttpRequest().cookies(this.cookies);
        return httpRequest.encode();
    }

    /**
     * @param packet
     * @return
     * @throws Exception
     * @author wesley
     * @see org.tio.core.intf.AioHandler#handler(org.tio.core.intf.Packet)
     */
    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        JoddHttpResponseWrap response = (JoddHttpResponseWrap) packet;
        HttpRequest httpRequest = responseHandler.handler(response.getJodddhttpResponse());
        if (httpRequest != null) {
            Aio.send(channelContext, new JoddHttpRequestWrap(httpRequest));
        }
        // requestHandler.handler(packet);
    }

    @Override
    public Packet heartbeatPacket() {
        jodd.http.HttpRequest httpRequest = jodd.http.HttpRequest.get("/");
        httpRequest.connectionKeepAlive(true);
        return new JoddHttpRequestWrap(httpRequest);
    }

}

package td.framework.boot.autoconfigure.tio.handler;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import jodd.http.Cookie;
import jodd.http.HttpRequest;
import td.framework.boot.autoconfigure.tio.packet.JoddHttpRequestWrap;
import td.framework.boot.autoconfigure.tio.packet.JoddHttpResponseWrap;

public class HttpClientAioHandler implements ClientAioHandler {
	private static Logger logger = LoggerFactory.getLogger(HttpClientAioHandler.class);

	public static final String REQUEST_KEY = "tio_request_key";

	private HttpResponseHandler responseHandler;

	private Cookie[] cookies;

	public HttpClientAioHandler(HttpResponseHandler responseHandler) {
		super();
		this.responseHandler = responseHandler;
	}

	/**
	 * @see org.tio.core.intf.AioHandler#decode(java.nio.ByteBuffer)
	 *
	 * @param buffer
	 * @return
	 * @throws AioDecodeException
	 * @author wesley
	 *
	 */
	@Override
	public JoddHttpResponseWrap decode(ByteBuffer buffer, ChannelContext channelContext) throws AioDecodeException {
		JoddHttpResponseWrap joddHttpResponseWrap = new JoddHttpResponseWrap(channelContext);
		joddHttpResponseWrap.decode(buffer);
		this.cookies = joddHttpResponseWrap.getJodddhttpResponse().cookies();
		return joddHttpResponseWrap;
	}

	/**
	 * @see org.tio.core.intf.AioHandler#encode(org.tio.core.intf.Packet)
	 *
	 * @param packet
	 * @return
	 * @author wesley
	 *
	 */
	@Override
	public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
		JoddHttpRequestWrap httpRequest = (JoddHttpRequestWrap) packet;
		if (this.cookies != null)
			httpRequest.getJoddHttpRequest().cookies(this.cookies);
		return httpRequest.encode();
	}

	/**
	 * @see org.tio.core.intf.AioHandler#handler(org.tio.core.intf.Packet)
	 *
	 * @param packet
	 * @return
	 * @throws Exception
	 * @author wesley
	 *
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

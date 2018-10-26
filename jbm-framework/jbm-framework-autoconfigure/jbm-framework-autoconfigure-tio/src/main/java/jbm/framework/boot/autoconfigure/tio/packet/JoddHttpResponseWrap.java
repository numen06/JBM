package jbm.framework.boot.autoconfigure.tio.packet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;

import jodd.http.HttpResponse;

public class JoddHttpResponseWrap extends Packet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HttpResponse jodddhttpResponse;
	private org.tio.http.common.HttpResponse tioHttpResponse;
	private final ChannelContext channelContext;

	public JoddHttpResponseWrap(ChannelContext channelContext) {
		super();
		this.channelContext = channelContext;
	}

	public HttpResponse getJodddhttpResponse() {
		return jodddhttpResponse;
	}

	public org.tio.http.common.HttpResponse getTioHttpResponse() {
		return tioHttpResponse;
	}

	public ChannelContext getChannelContext() {
		return channelContext;
	}

	public void decode(ByteBuffer byteBuffer) {
		final InputStream inputStream = new ByteArrayInputStream(byteBuffer.array());
		byteBuffer.limit(byteBuffer.array().length);
		byteBuffer.position(byteBuffer.array().length);
		jodd.http.HttpResponse httpResponse = jodd.http.HttpResponse.readFrom(inputStream);
//		this.tioHttpResponse = org.tio.http.common.HttpResponse.decode(ByteBuffer.wrap(httpResponse.toByteArray()), channelContext);
		this.jodddhttpResponse = httpResponse;
	}

}

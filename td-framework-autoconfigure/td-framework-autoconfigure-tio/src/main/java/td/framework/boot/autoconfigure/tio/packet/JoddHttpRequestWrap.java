package td.framework.boot.autoconfigure.tio.packet;

import java.nio.ByteBuffer;

import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;

import jodd.http.HttpRequest;

public class JoddHttpRequestWrap extends Packet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final HttpRequest joddHttpRequest;

	public JoddHttpRequestWrap(HttpRequest joddHttpRequest) {
		super();
		this.joddHttpRequest = joddHttpRequest;
	}

	public HttpRequest getJoddHttpRequest() {
		return joddHttpRequest;
	}

	public ByteBuffer encode() {
		return ByteBuffer.wrap(joddHttpRequest.toByteArray());
	}

}

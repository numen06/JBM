package td.framework.boot.autoconfigure.tio.listener;

import org.apache.commons.lang3.StringUtils;
import org.tio.client.intf.ClientAioListener;
import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.intf.Packet;
import org.tio.core.ssl.SslFacadeContext;
import org.tio.http.common.HttpConst;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;
import org.tio.http.server.HttpServerAioHandler;

public class HttpClientAioListener implements ClientAioListener {

	public HttpClientAioListener() {
		super();
	}

	@Override
	public void onAfterClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) {
	}

	@Override
	public void onAfterConnected(ChannelContext channelContext, boolean isConnected, boolean isReconnect) {
		// if (isConnected) {
		// String ip = channelContext.getClientNode().getIp();
		// AtomicLong ipcount = ipmap.get(ip);
		// if (ipcount == null) {
		// ipcount = new AtomicLong();
		// ipmap.put(ip, ipcount);
		// }
		// ipcount.incrementAndGet();
		//
		// String accessCountStr =
		// StringUtils.rightPad(accessCount.incrementAndGet() + "", 9);
		// String ipCountStr = StringUtils.rightPad(ipmap.size() + "", 9);
		// String ipStr = StringUtils.leftPad(ip, 15);
		// //地区，所有的访问次数，有多少个不同的ip， ip， 这个ip连接的次数
		// iplog.info("总访问次数:{}, 共有{}个不同ip访问, [{}]的访问次数{}, ", accessCountStr,
		// ipCountStr, ipStr, ipcount);
		// }

		return;
	}

	@Override
	public void onAfterReceived(ChannelContext channelContext, Packet packet, int packetSize) {
		// CommandStat.getCount(packet.getCommand()).received.incrementAndGet();
	}

	@Override
	public void onAfterSent(ChannelContext channelContext, Packet packet, boolean isSentSuccess) {
		// if (isSentSuccess) {
		// CommandStat.getCount(packet.getCommand()).sent.incrementAndGet();
		// }

		SslFacadeContext sslFacadeContext = channelContext.getSslFacadeContext();
		if ((sslFacadeContext == null || sslFacadeContext.isHandshakeCompleted()) && packet instanceof HttpResponse) {
			HttpResponse httpResponse = (HttpResponse) packet;

			String Connection = httpResponse.getHeader(HttpConst.ResponseHeaderKey.Connection);
			// 现在基本都是1.1了，所以用close来判断
			if (StringUtils.equalsIgnoreCase(Connection, HttpConst.ResponseHeaderValue.Connection.close)) {
				HttpRequest request = httpResponse.getHttpRequest();
				String line = request.getRequestLine().getLine();
				Aio.remove(channelContext, "onAfterSent, " + line);
			}
		}
	}

	@Override
	public void onBeforeClose(ChannelContext channelContext, Throwable throwable, String remark, boolean isRemove) {
		HttpRequest request = (HttpRequest) channelContext.getAttribute(HttpServerAioHandler.REQUEST_KEY);
		if (request != null) {
			request.setClosed(true);
		}
	}
}

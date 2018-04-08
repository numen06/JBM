package td.framework.boot.autoconfigure.tio.handler;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.client.intf.ClientAioHandler;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import td.framework.boot.autoconfigure.tio.code.JsonPacketDecode;
import td.framework.boot.autoconfigure.tio.code.JsonPacketEncode;
import td.framework.boot.autoconfigure.tio.packet.JsonForcer;
import td.framework.boot.autoconfigure.tio.packet.JsonHeartbeatPacket;
import td.framework.boot.autoconfigure.tio.packet.JsonPacket;

/**
 * 基于AIO的JSON传输协议
 * 
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 */
public class JsonClientAioHandler implements ClientAioHandler {
	private static final JsonPacket heartbeatPacket = new JsonHeartbeatPacket();

	private static JsonPacketEncode jsonPacketEncode = new JsonPacketEncode();
	private static JsonPacketDecode jsonPacketDecode = new JsonPacketDecode();

	private final static Logger logger = LoggerFactory.getLogger(JsonClientAioHandler.class);

	/**
	 * 解码：把接收到的ByteBuffer，解码成应用可以识别的业务消息包 总的消息结构：消息头 + 消息体 消息头结构： 4个字节，存储消息体的长度
	 * 消息体结构： 对象的json串的byte[]
	 */
	@Override
	public JsonPacket decode(ByteBuffer buffer, ChannelContext channelContext) throws AioDecodeException {
		return jsonPacketDecode.decode(buffer);
	}

	/**
	 * 编码：把业务消息包编码为可以发送的ByteBuffer 总的消息结构：消息头 + 消息体 消息头结构： 4个字节，存储消息体的长度 消息体结构：
	 * 对象的json串的byte[]
	 */
	@Override
	public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
		if (packet instanceof JsonPacket)
			return jsonPacketEncode.encode((JsonPacket) packet, groupContext);
		return null;
	}

	/**
	 * 处理消息
	 */
	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {
		try {
			JsonPacket jsonPacket = (JsonPacket) packet;
			if (jsonPacket.getJsonForcer().getType() == 0) {
				logger.debug("客户端收到心跳包");
				return;
			}
			logger.debug("群组：{},服务器ID：{},消息类型：{}", channelContext.getGroupContext().getId(), channelContext.getId(), jsonPacket.getJsonForcer().getTargetClass());
			this.handlerMessage(jsonPacket.getJsonForcer(), channelContext);
		} catch (Exception e) {
			logger.error("客户端消息处理错误", e);
		}
	}

	public void handlerMessage(JsonForcer jsonForcer, ChannelContext channelContext) throws Exception {

	}

	/**
	 * 此方法如果返回null，框架层面则不会发心跳；如果返回非null，框架层面会定时发本方法返回的消息包
	 */
	@Override
	public Packet heartbeatPacket() {
		return heartbeatPacket;
	}

}

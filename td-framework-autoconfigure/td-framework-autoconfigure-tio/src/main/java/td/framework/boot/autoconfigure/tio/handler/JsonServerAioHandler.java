package td.framework.boot.autoconfigure.tio.handler;

import java.nio.ByteBuffer;

import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioHandler;

import td.framework.boot.autoconfigure.tio.code.JsonPacketDecode;
import td.framework.boot.autoconfigure.tio.code.JsonPacketEncode;
import td.framework.boot.autoconfigure.tio.packet.JsonPacket;

public class JsonServerAioHandler implements ServerAioHandler {

	private static JsonPacketEncode jsonPacketEncode = new JsonPacketEncode();
	private static JsonPacketDecode jsonPacketDecode = new JsonPacketDecode();

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
		return jsonPacketEncode.encode((JsonPacket) packet, groupContext);
	}

	/**
	 * 处理消息
	 */
	@Override
	public void handler(Packet packet, ChannelContext channelContext) throws Exception {
		JsonPacket jsonPacket = (JsonPacket) packet;
		if (jsonPacket.getJsonForcer().getType() == 0) {
			System.out.println("服务器收到心跳包");
			return;
		}
		System.out.println("服务器收到消息：" + jsonPacket.getJsonForcer().toJavaBean());

		JsonPacket resppacket = new JsonPacket();

		resppacket.pack("我是服务器返回消息，你的消息是:" + jsonPacket.getJsonForcer().toJavaBean());
		Aio.send(channelContext, resppacket);
	}
}

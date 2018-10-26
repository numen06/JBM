package jbm.framework.boot.autoconfigure.tio.code;

import java.nio.ByteBuffer;

import org.tio.core.GroupContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import jbm.framework.boot.autoconfigure.tio.packet.JsonPacket;

public class JsonPacketEncode implements PacketEncode<JsonPacket> {

	@Override
	public ByteBuffer encode(JsonPacket packet, GroupContext groupContext) {

		byte[] body = null;
		int bodyLen = 0;
		// if (packet.getJsonBody() != null) {
		// body = packet.getJsonBody().getBytes(IOUtils.UTF8);
		// bodyLen = body.length;
		// }

		if (packet.getJsonForcer() != null) {
			body = JSON.toJSONBytes(packet.getJsonForcer(), SerializerFeature.DisableCircularReferenceDetect);
			bodyLen = body.length;
		}

		// bytebuffer的总长度是 = 消息头的长度 + 消息体的长度
		int allLen = JsonPacket.HEADER_LENGHT + bodyLen;
		// 创建一个新的bytebuffer
		ByteBuffer buffer = ByteBuffer.allocate(allLen);
		// 设置字节序
		buffer.order(groupContext.getByteOrder());

		// 写入消息头----消息头的内容就是消息体的长度
		buffer.putInt(bodyLen);

		// 写入消息体
		if (body != null) {
			buffer.put(body);
		}
		return buffer;
	}

}

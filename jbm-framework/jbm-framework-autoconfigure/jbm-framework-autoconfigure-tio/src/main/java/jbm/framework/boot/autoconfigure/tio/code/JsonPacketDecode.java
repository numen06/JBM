package jbm.framework.boot.autoconfigure.tio.code;

import jbm.framework.boot.autoconfigure.tio.packet.JsonPacket;
import org.tio.core.exception.AioDecodeException;

import java.nio.ByteBuffer;

public class JsonPacketDecode implements PacketDecode<JsonPacket> {

    @Override
    public JsonPacket decode(ByteBuffer buffer) throws AioDecodeException {
        int readableLength = buffer.limit() - buffer.position();
        // 收到的数据组不了业务包，则返回null以告诉框架数据不够
        if (readableLength < JsonPacket.HEADER_LENGHT) {
            return null;
        }

        // 读取消息体的长度
        int bodyLength = buffer.getInt();

        // 数据不正确，则抛出AioDecodeException异常
        if (bodyLength < 0) {
            throw new AioDecodeException("bodyLength [" + bodyLength + "] is not right");
        }

        // 计算本次需要的数据长度
        int neededLength = JsonPacket.HEADER_LENGHT + bodyLength;
        // 收到的数据是否足够组包
        int isDataEnough = readableLength - neededLength;
        // 不够消息体长度(剩下的buffe组不了消息体)
        if (isDataEnough < 0) {
            return null;
        } else // 组包成功
        {
            JsonPacket imPacket = new JsonPacket();
            if (bodyLength > 0) {
                byte[] dst = new byte[bodyLength];
                buffer.get(dst);
                imPacket.unPack(dst);
            }
            return imPacket;
        }
    }

}

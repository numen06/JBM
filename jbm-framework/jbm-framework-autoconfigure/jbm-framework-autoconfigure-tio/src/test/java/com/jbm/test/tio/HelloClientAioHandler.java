package com.jbm.test.tio;

import org.tio.client.intf.ClientAioHandler;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

public class HelloClientAioHandler implements ClientAioHandler {
    private static HelloPacket heartbeatPacket = new HelloPacket();

    /**
     * 解码：把接收到的ByteBuffer，解码成应用可以识别的业务消息包 总的消息结构：消息头 + 消息体 消息头结构： 4个字节，存储消息体的长度
     * 消息体结构： 对象的json串的byte[]
     */
    @Override
    public HelloPacket decode(ByteBuffer buffer, ChannelContext channelContext) throws AioDecodeException {
        int readableLength = buffer.limit() - buffer.position();
        // 收到的数据组不了业务包，则返回null以告诉框架数据不够
        if (readableLength < HelloPacket.HEADER_LENGHT) {
            return null;
        }

        // 读取消息体的长度
        int bodyLength = buffer.getInt();

        // 数据不正确，则抛出AioDecodeException异常
        if (bodyLength < 0) {
            throw new AioDecodeException("bodyLength [" + bodyLength + "] is not right, remote:" + channelContext.getClientNode());
        }

        // 计算本次需要的数据长度
        int neededLength = HelloPacket.HEADER_LENGHT + bodyLength;
        // 收到的数据是否足够组包
        int isDataEnough = readableLength - neededLength;
        // 不够消息体长度(剩下的buffe组不了消息体)
        if (isDataEnough < 0) {
            return null;
        } else // 组包成功
        {
            HelloPacket imPacket = new HelloPacket();
            if (bodyLength > 0) {
                byte[] dst = new byte[bodyLength];
                buffer.get(dst);
                imPacket.setBody(dst);
            }
            return imPacket;
        }
    }

    /**
     * 编码：把业务消息包编码为可以发送的ByteBuffer 总的消息结构：消息头 + 消息体 消息头结构： 4个字节，存储消息体的长度 消息体结构：
     * 对象的json串的byte[]
     */
    @Override
    public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
        HelloPacket helloPacket = (HelloPacket) packet;
        byte[] body = helloPacket.getBody();
        int bodyLen = 0;
        if (body != null) {
            bodyLen = body.length;
        }

        // bytebuffer的总长度是 = 消息头的长度 + 消息体的长度
        int allLen = HelloPacket.HEADER_LENGHT + bodyLen;
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

    /**
     * 处理消息
     */
    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        HelloPacket helloPacket = (HelloPacket) packet;
        byte[] body = helloPacket.getBody();
        if (body != null) {
            String str = new String(body, HelloPacket.CHARSET);
            System.out.println("收到消息：" + str);
        }

        return;
    }

    /**
     * 此方法如果返回null，框架层面则不会发心跳；如果返回非null，框架层面会定时发本方法返回的消息包
     */
    @Override
    public HelloPacket heartbeatPacket() {
        return heartbeatPacket;
    }
}
package jbm.framework.boot.autoconfigure.modbus.socket.handler;

import cn.hutool.core.io.BufferUtil;
import cn.hutool.core.util.StrUtil;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import jbm.framework.boot.autoconfigure.modbus.socket.ModbusDeviceContainer;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.HeartbeatPacket;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.ModbusRequestPacket;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.ModbusResponsePacket;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.RegisteredPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.Tio;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.EncodedPacket;
import org.tio.core.intf.Packet;
import org.tio.server.intf.ServerAioHandler;

import java.nio.ByteBuffer;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-04 02:40
 **/
@Slf4j
public class MServerAioHandler implements ServerAioHandler {

    @Autowired
    private ModbusDeviceContainer deviceService;

    @Override
    public Packet decode(ByteBuffer byteBuffer, int limit, int position, int readableLength, ChannelContext channelContext) throws AioDecodeException {
        final byte[] bytes = BufferUtil.readBytes(byteBuffer);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        log.info("Response: " + sb.toString());
        if (readableLength <= 2) {
            return new HeartbeatPacket();
        }
        if (StrUtil.isEmpty(channelContext.getBsId())) {
            String psn = new String(bytes);
            channelContext.setBsId(psn);
            return new RegisteredPacket(psn);
        }
        try {

            ModbusResponsePacket modbusResponsePacket = new ModbusResponsePacket(bytes);
            return modbusResponsePacket;
        } catch (ModbusTransportException e) {
//            log.error("不是modbus包", e);
            return new EncodedPacket(bytes);
        }
    }


    @Override
    public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
        if (packet instanceof ModbusRequestPacket) {
            ByteBuffer byteBuffer = ((ModbusRequestPacket) packet).build();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteBuffer.array()) {
                sb.append(String.format("%02X ", b));
            }
            log.info("Request: " + sb.toString());
            return byteBuffer;
        }
        return packet.getPreEncodedByteBuffer();
    }

    @Override
    public void handler(Packet packet, ChannelContext channelContext) throws Exception {
        try {
            if (packet instanceof RegisteredPacket) {
                log.info("RegisteredPacket", (RegisteredPacket) packet);
                deviceService.init((RegisteredPacket) packet, channelContext);
            } else if (packet instanceof ModbusRequestPacket) {
                log.info("ModbusRequestPacket", (ModbusRequestPacket) packet);
            } else if (packet instanceof ModbusResponsePacket) {
                deviceService.getModbusMasterMap().get(channelContext.getBsId()).response(((ModbusResponsePacket) packet).getModbusResponse());
                log.info("ModbusResponsePacket", (ModbusResponsePacket) packet);
            }
        } catch (Exception e) {
            Tio.close(channelContext, "解析失败断开连接");
        }

    }
}

package jbm.framework.boot.autoconfigure.modbus.socket.packet;

import com.serotonin.modbus4j.sero.messaging.OutgoingRequestMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-17 22:41
 **/
@Data
@Slf4j
public class ModbusRequestPacket extends Packet {

    private OutgoingRequestMessage modbusRequest;

    private byte[] data;

    public ModbusRequestPacket(OutgoingRequestMessage modbusRequest) {
        this.modbusRequest = modbusRequest;

    }

    public ModbusRequestPacket(byte[] data) {
        this.data = data;
    }


    public ByteBuffer build() {
        if (data == null) {
            this.data = modbusRequest.getMessageData();
//            log.info(modbusRequest.toString());
        }
        return ByteBuffer.wrap(data);
    }


}

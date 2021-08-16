package jbm.framework.boot.autoconfigure.modbus.socket.packet;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.xa.XaMessageResponse;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.util.queue.ByteQueue;
import lombok.Data;
import org.tio.core.intf.Packet;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-17 21:56
 **/
@Data
public class ModbusResponsePacket extends Packet {

    private IncomingResponseMessage modbusResponse;

    public ModbusResponsePacket(byte[] bytes) throws ModbusTransportException {
        modbusResponse = createXaMessageResponse(new ByteQueue(bytes));
    }

    private XaMessageResponse createXaMessageResponse(ByteQueue queue) throws ModbusTransportException {
        int transactionId = ModbusUtils.popShort(queue);
        int protocolId = ModbusUtils.popShort(queue);
        if (protocolId != 0) {
            throw new ModbusTransportException("Unsupported IP protocol id: " + protocolId);
        } else {
            ModbusUtils.popShort(queue);
            ModbusResponse response = ModbusResponse.createModbusResponse(queue);
            return new XaMessageResponse(response, transactionId);
        }
    }

}

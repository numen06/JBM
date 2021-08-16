package jbm.framework.boot.autoconfigure.modbus.socket;

import com.google.common.collect.Maps;
import com.serotonin.modbus4j.exception.ModbusInitException;
import jbm.framework.boot.autoconfigure.modbus.master.TioModbusMaster;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.RegisteredPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

import java.util.Map;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-22 00:43
 **/
@Slf4j
@Data
public class ModbusDeviceContainer {

    private Map<String, TioModbusMaster> modbusMasterMap = Maps.newConcurrentMap();

    public void init(RegisteredPacket registeredPacket, ChannelContext channelContext) {
        TioModbusMaster modbusMaster = new TioModbusMaster(channelContext);
        try {
            modbusMaster.init();
            modbusMasterMap.put(channelContext.getBsId(), modbusMaster);
        } catch (ModbusInitException e) {
            log.error("tio modbus master init error", e);
        }
        log.info("设备连接成功，PSN:[{}]", registeredPacket.getPsn());
    }
}

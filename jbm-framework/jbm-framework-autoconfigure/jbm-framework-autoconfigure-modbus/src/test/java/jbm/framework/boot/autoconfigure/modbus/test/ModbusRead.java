package jbm.framework.boot.autoconfigure.modbus.test;

import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.locator.BaseLocator;
import jbm.framework.boot.autoconfigure.modbus.socket.ModbusDeviceContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-17 22:28
 **/
@Service
@Slf4j
public class ModbusRead {

    @Autowired
    private ModbusDeviceContainer deviceService;


    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void timeRead() throws ModbusTransportException, ErrorResponseException {

        for (String psn : deviceService.getModbusMasterMap().keySet()) {
            log.info("start request psn data");
            ModbusMaster modbusMaster = deviceService.getModbusMasterMap().get(psn);

            BatchRead<Integer> batch = new BatchRead<Integer>();
            batch.addLocator(0x0046, BaseLocator.holdingRegister(1, 0x0046, DataType.TWO_BYTE_INT_SIGNED));
            batch.addLocator(0x0047, BaseLocator.holdingRegister(1, 0x0047, DataType.TWO_BYTE_INT_SIGNED));
            batch.addLocator(0x0048, BaseLocator.holdingRegister(1, 0x0048, DataType.TWO_BYTE_INT_SIGNED));
            batch.addLocator(0x004A, BaseLocator.holdingRegister(1, 0x004A, DataType.TWO_BYTE_INT_SIGNED));
            batch.addLocator(0x004B, BaseLocator.holdingRegister(1, 0x004B, DataType.TWO_BYTE_INT_SIGNED));
            batch.addLocator(0x004C, BaseLocator.holdingRegister(1, 0x004C, DataType.TWO_BYTE_INT_SIGNED));
            batch.setContiguousRequests(false);
//            ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(1, 75, 10);
            BatchResults<Integer> results = modbusMaster.send(batch);
            log.info("end request psn data:{}", results.toString());
        }
//        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(1, 75, 10);
////        Tio.sendToBsId(serverGroupContext, "PSN001", new ModbusRequestPacket(request));
////        log.info(JSON.toJSONString(tcpMaster.scanForSlaveNodes()));
////        Tio.sendToAll(serverGroupContext, new ModbusRequestPacket(request));
//
//        OutgoingRequestMessage ipRequest;
//        ipRequest = new XaMessageRequest(request, 0);
//        Tio.sendToAll(serverGroupContext, new ModbusRequestPacket(ipRequest));
    }
}

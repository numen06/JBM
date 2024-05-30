package com.jbm.framework.modbus;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.framework.modbus.event.ValueChangeEvent;
import com.serotonin.modbus4j.*;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author fanscat
 * @createTime 2024/5/29 11:39
 */
@Slf4j
public class ModbusTemplate {

    private Map<String, ModbusClientBean> MODBUS_CLIENT_MAP = new ConcurrentHashMap();

    private ScheduledThreadPoolExecutor SCHEDULED_THREAD_POOL_EXECUTOR;

    @Autowired
    private ApplicationContext applicationContext;

    public void loadClients(Map<String, ModbusSource> clients) {
        SCHEDULED_THREAD_POOL_EXECUTOR = ThreadUtil.createScheduledExecutor(clients.size());
        for (Map.Entry<String, ModbusSource> client : clients.entrySet()) {
            ModbusClientBean clientBean = new ModbusClientBean();
            clientBean.setClientId(client.getKey());
            clientBean.setSource(client.getValue());
            this.addClient(clientBean);
        }
    }

    private synchronized void addClient(ModbusClientBean clientBean) {
        ModbusMaster modbusMaster = this.getModbusMaster(clientBean.getClientId(), clientBean.getSource());
        clientBean.setClient(modbusMaster);
        MODBUS_CLIENT_MAP.put(clientBean.getClientId(), clientBean);
    }

    public Number readItem(String clientId, int slaveId, int offset) throws Exception {
        return this.getModbusMaster(clientId).getValue(BaseLocator.holdingRegister(slaveId, offset, DataType.TWO_BYTE_INT_SIGNED));
    }

    public BatchResults readItem(String clientId, int slaveId, int[] offset) throws Exception {
        List<BaseLocator<Number>> baseLocators = Arrays.stream(offset).mapToObj(i -> BaseLocator.holdingRegister(slaveId, i, DataType.TWO_BYTE_INT_SIGNED)).collect(Collectors.toList());
        BatchRead<Number> batchRead = new BatchRead<>();
        baseLocators.forEach(item -> batchRead.addLocator(item.getOffset(), item));
        return this.getModbusMaster(clientId).send(batchRead);
    }

    public short[] readItem(String clientId, int slaveId, int startOffset, int numberOfRegisters) throws Exception {
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startOffset, numberOfRegisters);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) this.getModbusMaster(clientId).send(request);
        if (response.isException()) {
            throw new ErrorResponseException(request, response);
        }
        return response.getShortData();
    }

    public void writeItem(String clientId, int slaveId, int writeOffset, int writeValue) throws Exception {
        WriteRegisterRequest request = new WriteRegisterRequest(slaveId, writeOffset, writeValue);
        WriteRegisterResponse response = (WriteRegisterResponse) this.getModbusMaster(clientId).send(request);
        if (response.isException()) {
            throw new ErrorResponseException(request, response);
        }
    }

    public void writeItem(String clientId, int slaveId, int point, short[] shorts) throws Exception {
        WriteRegistersRequest request = new WriteRegistersRequest(slaveId, point, shorts);
        WriteRegistersResponse response = (WriteRegistersResponse) this.getModbusMaster(clientId).send(request);
        if (response.isException()) {
            throw new ErrorResponseException(request, response);
        }
    }

    public void subscribeItem(String clientId, int slaveId, int startOffset, int numberOfRegisters) throws Exception {
        ValueChangeProcessImage processImage = new ValueChangeProcessImage(slaveId);
        processImage.setHoldingRegister(startOffset, new short[numberOfRegisters]);
        processImage.addListener(new ProcessImageListener() {

            @Override
            public void coilWrite(int offset, boolean oldValue, boolean newValue) {
                applicationContext.publishEvent(new ValueChangeEvent(this, clientId, offset, newValue));
            }

            @Override
            public void holdingRegisterWrite(int offset, short oldValue, short newValue) {
                applicationContext.publishEvent(new ValueChangeEvent(this, clientId, offset, newValue));
            }

        });
        ModbusMaster modbusMaster = this.getModbusMaster(clientId);
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startOffset, numberOfRegisters);
        ThreadUtil.schedule(SCHEDULED_THREAD_POOL_EXECUTOR, () -> {
            try {
                ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) modbusMaster.send(request);
                if (response.isException()) {
                    throw new ErrorResponseException(request, response);
                }
                processImage.writeHoldingRegisters(startOffset, response.getShortData());
            } catch (Exception var3) {
                log.error("modbus client [{}] error [{}]", clientId, var3.getMessage(), var3);
            }
        }, 500, 1000, Boolean.TRUE);
    }

    private ModbusMaster getModbusMaster(String clientId) {
        return this.getModbusMaster(clientId, null);
    }

    private ModbusMaster getModbusMaster(String clientId, ModbusSource modbusSource) {
        if (MODBUS_CLIENT_MAP.containsKey(clientId) && MODBUS_CLIENT_MAP.get(clientId).getClient() != null) {
            return MODBUS_CLIENT_MAP.get(clientId).getClient();
        } else if (ObjectUtil.isNotEmpty(modbusSource)) {
            IpParameters params = new IpParameters();
            params.setHost(modbusSource.getHost());
            params.setPort(modbusSource.getPort());
            params.setEncapsulated(modbusSource.getEncapsulated());
            ModbusMaster modbusMaster = new ModbusFactory().createTcpMaster(params, true);
            try {
                //设置超时时间
                modbusMaster.setTimeout(modbusSource.getTimeout());
                //设置重连次数
                modbusMaster.setRetries(modbusSource.getRetries());
                //初始化
                modbusMaster.init();
                return modbusMaster;
            } catch (ModbusInitException var3) {
                log.error("get modbus client [{}] error [{}]", clientId, var3.getMessage(), var3);
            }
        }
        return null;
    }
}

package com.jbm.framework.modbus;

import com.serotonin.modbus4j.ModbusMaster;
import lombok.Data;

/**
 * @author fanscat
 * @createTime 2024/5/29 16:05
 */
@Data
public class ModbusClientBean {
    private String clientId;
    private ModbusSource source;
    private ModbusMaster client;
}

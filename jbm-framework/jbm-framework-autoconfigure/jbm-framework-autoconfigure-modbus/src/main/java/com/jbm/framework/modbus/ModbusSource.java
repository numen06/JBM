package com.jbm.framework.modbus;

import lombok.Data;

/**
 * @author fanscat
 * @createTime 2024/5/29 11:35
 */
@Data
public class ModbusSource {
    private String host;
    private Integer port = 502;
    private Integer timeout = 3000;
    private Integer retries = 3;
    private Boolean encapsulated = false;
}

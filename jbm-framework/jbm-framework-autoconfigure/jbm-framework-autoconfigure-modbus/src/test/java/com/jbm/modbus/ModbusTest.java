package com.jbm.modbus;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import com.jbm.framework.modbus.ModbusTemplate;
import com.serotonin.modbus4j.BatchResults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

/**
 * @author fanscat
 * @createTime 2024/5/29 14:14
 */
@Slf4j
@SpringBootTest(classes = ModbusApplicationTest.class)
public class ModbusTest {

    public static final String CLIENT_NAME = "01";

    public static final int SLAVE_ID = 1, START_OFFSET = 0, NUMBER_OF_REGISTERS = 10;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void holdingRegister() throws Exception {
        Number value = this.applicationContext.getBean(ModbusTemplate.class).readItem(ModbusTest.CLIENT_NAME, ModbusTest.SLAVE_ID, ModbusTest.START_OFFSET);
        log.info("设备[{}]信号[{}]读取值:{}", ModbusTest.CLIENT_NAME, ModbusTest.START_OFFSET, value);
    }

    @Test
    public void batchHoldingRegister() throws Exception {
        int[] offset = ArrayUtil.range(ModbusTest.START_OFFSET, ModbusTest.NUMBER_OF_REGISTERS);
        BatchResults<Number> results = this.applicationContext.getBean(ModbusTemplate.class).readItem(ModbusTest.CLIENT_NAME, ModbusTest.SLAVE_ID, offset);
        Arrays.stream(offset).forEach(i -> log.info("设备[{}]信号[{}]读取值:{}", ModbusTest.CLIENT_NAME, i, results.getValue(i)));
    }

    @Test
    public void readHoldingRegistersRequest() throws Exception {
        short[] shorts = this.applicationContext.getBean(ModbusTemplate.class).readItem(ModbusTest.CLIENT_NAME, ModbusTest.SLAVE_ID, ModbusTest.START_OFFSET, ModbusTest.NUMBER_OF_REGISTERS);
        log.info("设备[{}]信号[{}]==>[{}]读取值:{}", ModbusTest.CLIENT_NAME, ModbusTest.START_OFFSET, ModbusTest.NUMBER_OF_REGISTERS, shorts);
    }
}

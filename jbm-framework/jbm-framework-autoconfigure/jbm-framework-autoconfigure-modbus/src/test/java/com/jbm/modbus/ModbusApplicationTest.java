package com.jbm.modbus;

import cn.hutool.core.util.ArrayUtil;
import com.jbm.framework.modbus.ModbusTemplate;
import com.jbm.framework.modbus.event.ValueChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

/**
 * @author fanscat
 * @createTime 2024/5/29 16:19
 */
@Slf4j
@SpringBootApplication
public class ModbusApplicationTest {
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(ModbusApplicationTest.class, args);
//        applicationContext.getBean(ModbusTemplate.class).subscribeItem(ModbusTest.CLIENT_NAME, ModbusTest.SLAVE_ID, ModbusTest.START_OFFSET, ModbusTest.NUMBER_OF_REGISTERS);
        int[] offset = ArrayUtil.range(ModbusTest.START_OFFSET, ModbusTest.START_OFFSET + ModbusTest.NUMBER_OF_REGISTERS);
        applicationContext.getBean(ModbusTemplate.class).subscribeItem(ModbusTest.CLIENT_NAME, ModbusTest.SLAVE_ID, offset);
    }

    @EventListener
    public void changeEvent(ValueChangeEvent changeEvent) {
        log.info("modbus client [{}] offset [{}] value [{}]", changeEvent.getClientId(), changeEvent.getOffset(), changeEvent.getValue());
    }
}

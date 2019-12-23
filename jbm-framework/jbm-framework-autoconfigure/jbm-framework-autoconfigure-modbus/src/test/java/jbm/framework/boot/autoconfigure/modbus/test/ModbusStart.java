package jbm.framework.boot.autoconfigure.modbus.test;

import jbm.framework.boot.autoconfigure.modbus.ModbusServerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-04 01:16
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(classes = {ModbusServerConfiguration.class})
//@ComponentScan("com.okc.modbus")
public class ModbusStart {
    @Test
    public void exampleTest1() {
    }
}

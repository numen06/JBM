package jbm.framework.boot.autoconfigure.modbus.test;

import jbm.framework.boot.autoconfigure.modbus.service.ModbusService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:21
 **/
@SpringBootApplication
@ComponentScan("jbm.framework.boot.autoconfigure.modbus")
public class Easymodbus4jTest {


    public static void main(String[] args) {
        SpringApplication.run(Easymodbus4jTest.class, args);
    }
}

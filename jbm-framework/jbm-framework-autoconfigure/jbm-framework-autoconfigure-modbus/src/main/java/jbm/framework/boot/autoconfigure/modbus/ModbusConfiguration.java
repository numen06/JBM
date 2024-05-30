package jbm.framework.boot.autoconfigure.modbus;

import com.jbm.framework.modbus.ModbusTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author fanscat
 * @createTime 2024/5/28 20:55
 */
@EnableConfigurationProperties(ModbusProperties.class)
public class ModbusConfiguration {

    @Bean
    private ModbusTemplate modbusTemplate(ModbusProperties modbusProperties) {
        ModbusTemplate modbusTemplate = new ModbusTemplate();
        modbusTemplate.loadClients(modbusProperties.getClients());
        return modbusTemplate;
    }
}

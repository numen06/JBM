package jbm.framework.boot.autoconfigure.modbus;

import com.jbm.framework.modbus.ModbusSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fanscat
 * @createTime 2024/5/28 20:54
 */
@Data
@ConfigurationProperties(prefix = "modbus")
public class ModbusProperties {
    private Map<String, ModbusSource> clients = new HashMap<>();
}

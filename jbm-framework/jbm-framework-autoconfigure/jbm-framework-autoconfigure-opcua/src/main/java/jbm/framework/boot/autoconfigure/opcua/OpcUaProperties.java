package jbm.framework.boot.autoconfigure.opcua;

import com.jbm.framework.opcua.OpcUaSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "opcua")
@Data
public class OpcUaProperties {

    private Map<String, OpcUaSource> clients = new HashMap<>();

}

package jbm.framework.boot.autoconfigure.modbus.master;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.ip.IpParameters;
import org.springframework.context.annotation.Bean;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-04 01:19
 **/
public class TcpMasterConfiguration {


    private static ModbusFactory modbusFactory = new ModbusFactory();

    /**
     * 获取master
     *
     * @return master
     */
    @Bean
    public ModbusMaster getMaster() {
        IpParameters params = new IpParameters();
        params.setHost("192.168.8.104");
        params.setPort(502);
        params.setEncapsulated(false);
        ModbusMaster master = modbusFactory.createTcpListener(params);// TCP 协议
        try {
            //设置超时时间
            master.setTimeout(3000);
            //设置重连次数
            master.setRetries(3);
            //初始化
            master.init();

//            master.setConnected(true);
        } catch (ModbusInitException e) {
            e.printStackTrace();
        }
        return master;
    }


}

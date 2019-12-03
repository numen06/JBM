package jbm.framework.boot.autoconfigure.modbus.service;

import com.github.zengfr.easymodbus4j.ModbusConfs;
import com.github.zengfr.easymodbus4j.ModbusConsts;
import com.github.zengfr.easymodbus4j.common.util.ConsoleUtil;
import com.github.zengfr.easymodbus4j.common.util.ScheduledUtil;
import com.github.zengfr.easymodbus4j.processor.ModbusMasterResponseProcessor;
import com.github.zengfr.easymodbus4j.processor.ModbusSlaveRequestProcessor;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import jbm.framework.boot.autoconfigure.modbus.config.ModbusConfig;
import jbm.framework.boot.autoconfigure.modbus.config.ModbusMasterSchedule4ConfigFile;
import jbm.framework.boot.autoconfigure.modbus.process.ExampleModbusMasterResponseProcessor;
import jbm.framework.boot.autoconfigure.modbus.process.ExampleModbusSlaveRequestProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:49
 **/
@Slf4j
@Service
public class ModbusService {

    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(ModbusService.class);

    @PostConstruct
    public void initAndStart() throws Exception {
        ModbusConfig config = new ModbusConfig();
        config.type = 2;
        config.host = "192.168.1.31";
        config.autoSend = true;
        config.unit_IDENTIFIER = 1;
        config.heartbeat = "12000";
        config.port = 502;
        start(config);

    }

    public void start(ModbusConfig cfg) throws Exception {
        ModbusConsts.DEFAULT_UNIT_IDENTIFIER = cfg.unit_IDENTIFIER;
        ModbusConsts.HEARTBEAT = cfg.heartbeat;

        ModbusConfs.MASTER_SHOW_DEBUG_LOG = cfg.showDebugLog;
        ModbusConfs.SLAVE_SHOW_DEBUG_LOG = cfg.showDebugLog;
        ModbusConfs.IDLE_TIMEOUT_SECOND = cfg.idleTimeOut;
        ModbusConfs.RESPONS_EFRAME_IGNORE_LENGTH_THRESHOLD = cfg.ignoreLengthThreshold;

        ModbusMasterResponseProcessor masterProcessor = new ExampleModbusMasterResponseProcessor(cfg.transactionIdentifierOffset);
        ModbusSlaveRequestProcessor slaveProcessor = new ExampleModbusSlaveRequestProcessor(cfg.transactionIdentifierOffset);

        ModbusSetup setup = new ModbusSetup();
        setup.initProperties();
        setup.initHandler(masterProcessor, slaveProcessor);

        int port = cfg.port;
        boolean autoSend = cfg.autoSend;
        String host = cfg.host;
        int sleep = cfg.sleep;
        switch (cfg.type) {

            case 6:
                setup.setupServer4RtuMaster(port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusServer().getChannels());
                break;
            case 7:
                setup.setupClient4RtuSlave(host, port);
                break;
            case 8:
                setup.setupClient4RtuMaster(host, port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusClient().getChannels());
                break;
            case 9:
                setup.setupServer4RtuSlave(port);
                break;
            case 5:
                ModbusConfs.SLAVE_SHOW_DEBUG_LOG = false;
                setup.setupServer4RtuMaster(port);
                setup.setupClient4RtuSlave(host, port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusServer().getChannels());
                break;

            case 1:
                setup.setupServer4TcpMaster(port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusServer().getChannels());
                break;
            case 2:
                setup.setupClient4TcpSlave(host, port);
                break;
            case 3:
                setup.setupClient4TcpMaster(host, port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusClient().getChannels());
                break;
            case 4:
                setup.setupServer4TcpSlave(port);
                break;
            default:
                ModbusConfs.SLAVE_SHOW_DEBUG_LOG = false;
                setup.setupServer4TcpMaster(port);
                setup.setupClient4TcpSlave(host, port);
                sendRequests4Auto(autoSend, sleep, setup.getModbusServer().getChannels());
                break;
        }
        Runnable runnable = () -> ConsoleUtil.clearConsole(true);
        ScheduledUtil.scheduleWithFixedDelay(runnable, sleep * 5);
    }

    protected void sendRequests4Auto(boolean autoSend, int sleep, Collection<Channel> channels) throws InterruptedException {
        if (autoSend) {
            ModbusMasterSchedule4ConfigFile modbusMasterAutoSender4ConfigFile = new ModbusMasterSchedule4ConfigFile();
            modbusMasterAutoSender4ConfigFile.schedule(channels, sleep);
        }
    }
}

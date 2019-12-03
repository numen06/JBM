package jbm.framework.boot.autoconfigure.modbus.config;

import com.alibaba.fastjson.JSON;
import com.github.zengfr.easymodbus4j.ModbusConsts;
import com.github.zengfr.easymodbus4j.common.util.ParseStringUtil;
import com.github.zengfr.easymodbus4j.ModbusConfs;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:46
 **/
public class ModbusConfig {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ModbusConfig.class);
    public int type;
    public String host;
    public int port;
    public boolean showDebugLog;
    public boolean autoSend;
    public int idleTimeOut;
    public int sleep;

    public short unit_IDENTIFIER;
    public short transactionIdentifierOffset;
    public int ignoreLengthThreshold;
    public String heartbeat;
    public int scheduleFixedDelay;
    public int udpPort;

    public static ModbusConfig parse(String[] argsArray) {
        ModbusConfig cfg = new ModbusConfig();

        cfg.type = ParseStringUtil.parseInt(argsArray, 0, 0);
        cfg.host = ParseStringUtil.parseString(argsArray, 1, ModbusConsts.DEFAULT_HOTST_IP);
        cfg.port = ParseStringUtil.parseInt(argsArray, 2, ModbusConfs.DEFAULT_MODBUS_PORT);
        cfg.unit_IDENTIFIER = ParseStringUtil.parseShort(argsArray, 3, ModbusConsts.DEFAULT_UNIT_IDENTIFIER);
        cfg.transactionIdentifierOffset = ParseStringUtil.parseShort(argsArray, 4, (short) 0);
        cfg.showDebugLog = ParseStringUtil.parseBoolean(argsArray, 5, true);
        cfg.idleTimeOut = ParseStringUtil.parseInt(argsArray, 6, ModbusConfs.IDLE_TIMEOUT_SECOND);

        cfg.autoSend = ParseStringUtil.parseBoolean(argsArray, 7, true);
        cfg.sleep = ParseStringUtil.parseInt(argsArray, 8, 1000 * 15);
        cfg.heartbeat = ParseStringUtil.parseString(argsArray, 9, ModbusConsts.HEARTBEAT);
        cfg.ignoreLengthThreshold = ParseStringUtil.parseInt(argsArray, 10, ModbusConfs.RESPONS_EFRAME_IGNORE_LENGTH_THRESHOLD);

        cfg.udpPort = ParseStringUtil.parseInt(argsArray, 11, ModbusConfs.DEFAULT_MODBUS_PORT5);
        logger.info(JSON.toJSONString(cfg));
        return cfg;
    }
}
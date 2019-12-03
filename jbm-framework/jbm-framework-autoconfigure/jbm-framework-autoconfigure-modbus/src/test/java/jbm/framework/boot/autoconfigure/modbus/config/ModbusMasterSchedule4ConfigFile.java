package jbm.framework.boot.autoconfigure.modbus.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.zengfr.easymodbus4j.common.util.FileUtil;
import com.github.zengfr.easymodbus4j.common.util.InputStreamUtil;
import com.github.zengfr.easymodbus4j.schedule.ModbusMasterSchedule;
import com.github.zengfr.easymodbus4j.sender.util.ModbusRequestSendUtil;
import com.google.common.collect.Lists;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:50
 **/
@Slf4j
public class ModbusMasterSchedule4ConfigFile extends ModbusMasterSchedule {
    private static final InternalLogger logger = InternalLoggerFactory
            .getInstance(ModbusMasterSchedule4ConfigFile.class);

    protected static String configFileName = "autoSend.txt";
    @Override
    protected int getFixedDelay() {
        return 0;
    }
    @Override
    protected ModbusRequestSendUtil.PriorityStrategy getPriorityStrategy() {
        return ModbusRequestSendUtil.PriorityStrategy.Channel;
    }
    @Override
    protected InternalLogger getLogger() {

        return logger;
    }

    @Override
    protected List<String> buildReqsList() {
        return parseReqs();
    }

    protected static List<String> parseReqs() {
        List<String> configStrings = readConfig("/" + configFileName);
        List<String> reqStrings = parseReqs(configStrings);
        return reqStrings;
    }

    protected static List<String> parseReqs(List<String> configStrings) {
        List<String> reqStrings = Lists.newArrayList(configStrings);
        if (!configStrings.isEmpty()) {
            reqStrings.remove(0);
        }
        return reqStrings;
    }

    protected static List<String> readConfig(String fileName) {
        logger.info("readConfig:" + fileName);
        List<String> strList = Lists.newArrayList();
        try {
            InputStream input = InputStreamUtil.getStream(fileName);
            if (input != null)
                strList = FileUtil.readLines(input, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.error("readConfig:" + fileName + " ex:", ex);
        }
        return strList;
    }
}

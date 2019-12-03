package jbm.framework.boot.autoconfigure.modbus.service;

import com.github.zengfr.easymodbus4j.client.ModbusClient;
import com.github.zengfr.easymodbus4j.client.ModbusClientRtuFactory;
import com.github.zengfr.easymodbus4j.client.ModbusClientTcpFactory;
import com.github.zengfr.easymodbus4j.handle.impl.ModbusMasterResponseHandler;
import com.github.zengfr.easymodbus4j.handle.impl.ModbusSlaveRequestHandler;
import com.github.zengfr.easymodbus4j.handler.ModbusRequestHandler;
import com.github.zengfr.easymodbus4j.handler.ModbusResponseHandler;
import com.github.zengfr.easymodbus4j.processor.ModbusMasterResponseProcessor;
import com.github.zengfr.easymodbus4j.processor.ModbusSlaveRequestProcessor;
import com.github.zengfr.easymodbus4j.server.ModbusServer;
import com.github.zengfr.easymodbus4j.server.ModbusServerRtuFactory;
import com.github.zengfr.easymodbus4j.server.ModbusServerTcpFactory;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-03 11:39
 **/
public class ModbusSetup {
    private static final int sleep = 1000;
    private ModbusClient modbusClient;
    private ModbusServer modbusServer;

    private ModbusRequestHandler requestHandler;
    private ModbusResponseHandler responseHandler;

    public ModbusSetup() {

    }

    public ModbusClient getModbusClient() {
        return this.modbusClient;
    }

    public ModbusServer getModbusServer() {
        return this.modbusServer;
    }

    public void initProperties() throws Exception {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        System.setProperty("io.netty.tryReflectionSetAccessible", "true");
        // System.setProperty("io.netty.noUnsafe", "false");
        // ReferenceCountUtil.release(byteBuf);
        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
    }

    public void initHandler(ModbusResponseHandler responseHandler, ModbusRequestHandler requestHandler) throws Exception {
        this.requestHandler = requestHandler;
        this.responseHandler = responseHandler;
    }

    public void initHandler(ModbusMasterResponseProcessor masterProcessor, ModbusSlaveRequestProcessor slaveProcessor) throws Exception {
        this.requestHandler = new ModbusSlaveRequestHandler(slaveProcessor);
        this.responseHandler = new ModbusMasterResponseHandler(masterProcessor);
    }

    public void setupServer4TcpMaster(int port) throws Exception {
        modbusServer = ModbusServerTcpFactory.getInstance().createServer4Master(port, responseHandler);
    }

    public void setupServer4TcpSlave(int port) throws Exception {
        modbusServer = ModbusServerTcpFactory.getInstance().createServer4Slave(port, requestHandler);

    }

    public void setupClient4TcpSlave(String host, int port) throws Exception {
        Thread.sleep(sleep);
        modbusClient = ModbusClientTcpFactory.getInstance().createClient4Slave(host, port, requestHandler);
    }

    public void setupClient4TcpMaster(String host, int port) throws Exception {
        Thread.sleep(sleep);
        modbusClient = ModbusClientTcpFactory.getInstance().createClient4Master(host, port, responseHandler);
    }

    public void setupServer4RtuMaster(int port) throws Exception {
        modbusServer = ModbusServerRtuFactory.getInstance().createServer4Master(port, responseHandler);
    }

    public void setupServer4RtuSlave(int port) throws Exception {
        modbusServer = ModbusServerRtuFactory.getInstance().createServer4Slave(port, requestHandler);

    }

    public void setupClient4RtuSlave(String host, int port) throws Exception {
        Thread.sleep(sleep);
        modbusClient = ModbusClientRtuFactory.getInstance().createClient4Slave(host, port, requestHandler);
    }

    public void setupClient4RtuMaster(String host, int port) throws Exception {
        Thread.sleep(sleep);
        modbusClient = ModbusClientRtuFactory.getInstance().createClient4Master(host, port, responseHandler);
    }
}
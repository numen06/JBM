package jbm.framework.boot.autoconfigure.modbus;

import jbm.framework.boot.autoconfigure.modbus.socket.ModbusDeviceContainer;
import jbm.framework.boot.autoconfigure.modbus.socket.handler.MServerAioHandler;
import jbm.framework.boot.autoconfigure.modbus.socket.handler.MServerAioListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.server.ServerGroupContext;
import org.tio.server.TioServer;

import java.io.IOException;


/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-04 02:11
 **/
@Slf4j
@Configuration
public class ModbusServerConfiguration {

    //有时候需要绑定ip，不需要则null
    private final String serverIp = "0.0.0.0";

    //监听的端口
    private final int serverPort = 502;

    @Bean
    public MServerAioHandler mServerAioHandler() {
        return new MServerAioHandler();
    }

    @Bean
    public MServerAioListener mServerAioListener() {
        return new MServerAioListener();
    }

    @Bean
    public ServerGroupContext serverGroupContext(MServerAioHandler mServerAioHandler, MServerAioListener mServerAioListener) {
        ServerGroupContext serverGroupContext = new ServerGroupContext("modbus-server", mServerAioHandler, mServerAioListener);
        return serverGroupContext;
    }


    @Bean
    public TioServer tioServer(ServerGroupContext serverGroupContext) {
        TioServer tioServer = new TioServer(serverGroupContext);
        init(tioServer);
        return tioServer;
    }

    @Bean
    public ModbusDeviceContainer deviceService() {
        return new ModbusDeviceContainer();
    }


    public void init(TioServer tioServer) {
        try {
            tioServer.start(serverIp, serverPort);
            log.info("Modbus Server启动成功，端口：" + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

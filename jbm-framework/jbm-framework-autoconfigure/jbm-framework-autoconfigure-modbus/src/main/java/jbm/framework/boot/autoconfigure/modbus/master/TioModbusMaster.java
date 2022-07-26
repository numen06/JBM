package jbm.framework.boot.autoconfigure.modbus.master;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpMessageResponse;
import com.serotonin.modbus4j.ip.xa.XaMessageRequest;
import com.serotonin.modbus4j.ip.xa.XaWaitingRoomKeyFactory;
import com.serotonin.modbus4j.msg.ModbusRequest;
import com.serotonin.modbus4j.msg.ModbusResponse;
import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import jbm.framework.boot.autoconfigure.modbus.socket.MessageSendControl;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

import java.util.Arrays;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-18 00:12
 **/
@Slf4j
public class TioModbusMaster extends ModbusMaster {
    private final ChannelContext channelContext;
    private int nextTransactionId = 0;
    private MessageSendControl messageSendControl;


    public TioModbusMaster(ChannelContext channelContext) {
        this.channelContext = channelContext;
    }


    @Override
    synchronized public void init() throws ModbusInitException {
        log.debug("Init TcpListener Port: " + channelContext.getClientNode());
        initialized = true;
        messageSendControl = new MessageSendControl(channelContext, new XaWaitingRoomKeyFactory());
        log.warn("Initialized Port: " + channelContext.getClientNode());
    }


    @Override
    public void destroy() {
        Tio.close(channelContext, "主动关闭连接");
    }


    public void response(IncomingResponseMessage modbusResponse) throws WaitingRoomException {
        messageSendControl.getWaitingRoom().response(modbusResponse);
    }


    @Override
    public ModbusResponse sendImpl(ModbusRequest request) throws ModbusTransportException {
        XaMessageRequest ipRequest;
        ipRequest = new XaMessageRequest(request, getNextTransactionId());
        StringBuilder sb = new StringBuilder();
        for (byte b : Arrays.copyOfRange(ipRequest.getMessageData(), 0, ipRequest.getMessageData().length)) {
            sb.append(String.format("%02X ", b));
        }
        log.debug("Xa Request: " + sb.toString());


        // Send the request to get the response.
        IpMessageResponse ipResponse;
        try {
            //通过Tio发送
            ipResponse = (IpMessageResponse) messageSendControl.send(ipRequest, 3000, 1);
            if (ipResponse == null) {
                throw new ModbusTransportException(new Exception("No valid response from slave!"), request.getSlaveId());
            }
            sb = new StringBuilder();
            for (byte b : Arrays.copyOfRange(ipResponse.getMessageData(), 0, ipResponse.getMessageData().length)) {
                sb.append(String.format("%02X ", b));
            }
            log.debug("Response: " + sb.toString());
            return ipResponse.getModbusResponse();

        } catch (Exception e) {
            throw new ModbusTransportException(e, request.getSlaveId());
        }
    }


    protected int getNextTransactionId() {
        return nextTransactionId++;
    }


}

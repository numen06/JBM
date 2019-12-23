package jbm.framework.boot.autoconfigure.modbus.socket;

import com.serotonin.modbus4j.sero.io.StreamUtils;
import com.serotonin.modbus4j.sero.messaging.*;
import jbm.framework.boot.autoconfigure.modbus.socket.packet.ModbusRequestPacket;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;
import org.tio.core.Tio;

import java.io.IOException;

/**
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-20 12:58
 **/
@Data
@Slf4j
public class MessageSendControl implements DataConsumer {
    private static int DEFAULT_RETRIES = 2;
    private static int DEFAULT_TIMEOUT = 500;
    private WaitingRoomKeyFactory waitingRoomKeyFactory;
    public final WaitingRoom waitingRoom = new WaitingRoom();
    private final ChannelContext channelContext;

    public MessageSendControl(ChannelContext channelContext, WaitingRoomKeyFactory waitingRoomKeyFactory) {
        this.channelContext = channelContext;
        this.waitingRoomKeyFactory = waitingRoomKeyFactory;
    }


    public IncomingResponseMessage send(OutgoingRequestMessage request, int timeout, int retries) throws IOException {
        byte[] data = request.getMessageData();
        if (log.isDebugEnabled())
            System.out.println("MessagingControl.send: " + StreamUtils.dumpHex(data));

        IncomingResponseMessage response = null;

        if (request.expectsResponse()) {
            WaitingRoomKey key = waitingRoomKeyFactory.createWaitingRoomKey(request);

            waitingRoom.setKeyFactory(waitingRoomKeyFactory);
            // Enter the waiting room
            waitingRoom.enter(key);

            try {
                do {
                    // Send the request.
                    write(data);

                    // Wait for the response.
                    response = waitingRoom.getResponse(key, timeout);

                    if (log.isDebugEnabled())
                        System.out.println("Timeout waiting for response");
                }
                while (response == null && retries-- > 0);
            } finally {
                // Leave the waiting room.
                waitingRoom.leave(key);
            }

            if (response == null)
                throw new TimeoutException("request=" + request);
        } else
            write(data);

        return response;

    }


    public void write(byte[] data) {
        Tio.bSend(channelContext, new ModbusRequestPacket(data));
    }


    @Override
    public void data(byte[] b, int len) {

    }

    @Override
    public void handleIOException(IOException e) {

    }
}

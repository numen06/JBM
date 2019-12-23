package jbm.framework.boot.autoconfigure.modbus.socket;

import com.serotonin.modbus4j.sero.messaging.IncomingResponseMessage;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKey;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomKeyFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-12-23 12:03
 **/
@Slf4j
public class WaitingRoom {

    private final Map<WaitingRoomKey, WaitingRoom.Member> waitHere = new HashMap<WaitingRoomKey, WaitingRoom.Member>();

    private WaitingRoomKeyFactory keyFactory;

    public void setKeyFactory(WaitingRoomKeyFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    /**
     * The request message should be sent AFTER entering the waiting room so that the (vanishingly small) chance of a
     * response being returned before the thread is waiting for it is eliminated.
     *
     * @return
     */
    public void enter(WaitingRoomKey key) {
       WaitingRoom.Member member = new WaitingRoom.Member();
        synchronized (this) {
            while (waitHere.get(key) != null) {
                if (log.isDebugEnabled())
                    log.debug("Duplicate waiting room key found. Waiting for member to leave.");
                try {
                    wait();
                } catch (InterruptedException e) {
                    // no op
                }
            }
            //            Member dup = waitHere.get(key);
            //            if (dup != null) {
            //
            //                throw new WaitingRoomException("Waiting room too crowded. Already contains the key " + key);
            //            }

            waitHere.put(key, member);
        }
    }

    public IncomingResponseMessage getResponse(WaitingRoomKey key, long timeout) throws WaitingRoomException {
        // Get the member.
       WaitingRoom.Member member;
        synchronized (this) {
            member = waitHere.get(key);
        }

        if (member == null)
            throw new WaitingRoomException("No member for key " + key);

        // Wait for the response.
        return member.getResponse(timeout);
    }

    public void leave(WaitingRoomKey key) {
        // Leave the waiting room
        synchronized (this) {
            waitHere.remove(key);

            // Notify any threads that are waiting to get in. This could probably be just a notify() call.
            notifyAll();
        }
    }

    /**
     * This method is used by the data listening thread to post responses as they are received from the transport.
     *
     * @param response the response message
     * @throws WaitingRoomException
     */
    public void response(IncomingResponseMessage response) throws WaitingRoomException {
        WaitingRoomKey key = keyFactory.createWaitingRoomKey(response);
        if (key == null)
            // The key factory can return a null key if the response should be ignored.
            return;

       WaitingRoom.Member member;

        synchronized (this) {
            member = waitHere.get(key);
        }

        if (member != null)
            member.setResponse(response);
        else
            throw new WaitingRoomException("No recipient was found waiting for response for key " + key);
    }

    /**
     * This class is used by network message controllers to manage the blocking of threads sending confirmed messages.
     * The instance itself serves as a monitor upon which the sending thread can wait (with a timeout). When a response
     * is received, the message controller can set it in here, automatically notifying the sending thread that the
     * response is available.
     *
     * @author Matthew Lohbihler
     */
    class Member {
        private IncomingResponseMessage response;

        synchronized void setResponse(IncomingResponseMessage response) {
            this.response = response;
            notify();
        }

        synchronized IncomingResponseMessage getResponse(long timeout) {
            // Check if there is a response object now.
            if (response != null)
                return response;

            // If not, wait the timeout and then check again.
            waitNoThrow(timeout);
            return response;
        }

        private void waitNoThrow(long timeout) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}

package jbm.framework.boot.autoconfigure.modbus.socket.packet;

import lombok.Data;
import org.tio.core.intf.Packet;

/**
 * 注册包
 *
 * @program: okc-emc-parent
 * @author: wesley.zhang
 * @create: 2019-12-17 23:26
 **/
@Data
public class RegisteredPacket extends Packet {

    private String psn;

    public RegisteredPacket(String psn) {
        this.psn = psn;
    }


}

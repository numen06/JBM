package jbm.framework.boot.autoconfigure.tio.code;

import org.tio.core.GroupContext;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * @param <T>
 * @author wesley.zhang
 * @version 1.0
 * @date 2017年11月7日
 */
public interface PacketEncode<T extends Packet> {
    public ByteBuffer encode(T buffer, GroupContext groupContext);
}

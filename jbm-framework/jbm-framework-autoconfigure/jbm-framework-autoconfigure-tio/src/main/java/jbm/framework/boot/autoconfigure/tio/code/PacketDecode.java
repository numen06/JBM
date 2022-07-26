package jbm.framework.boot.autoconfigure.tio.code;

import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * @param <T>
 * @author wesley.zhang
 * @version 1.0
 * @date 2017年11月7日
 */
public interface PacketDecode<T extends Packet> {
    public T decode(ByteBuffer buffer) throws AioDecodeException;
}

package jbm.framework.boot.autoconfigure.tio.code;

import java.nio.ByteBuffer;

import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;

/**
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 * @param <T>
 */
public interface PacketDecode<T extends Packet> {
	public T decode(ByteBuffer buffer) throws AioDecodeException;
}

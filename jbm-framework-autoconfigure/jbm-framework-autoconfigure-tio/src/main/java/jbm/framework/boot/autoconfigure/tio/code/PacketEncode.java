package jbm.framework.boot.autoconfigure.tio.code;

import java.nio.ByteBuffer;

import org.tio.core.GroupContext;
import org.tio.core.intf.Packet;

/**
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 * @param <T>
 */
public interface PacketEncode<T extends Packet> {
	public ByteBuffer encode(T buffer, GroupContext groupContext);
}

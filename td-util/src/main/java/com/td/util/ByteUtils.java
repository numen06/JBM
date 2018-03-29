package com.td.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 字节处理工具类,高低位处理
 * 
 * @author wesley
 *
 */
public class ByteUtils {

	public static byte[] int2bytes(int i) {
		byte[] b = new byte[4];
		b[0] = (byte) (0xff & i);
		b[1] = (byte) ((0xff00 & i) >> 8);
		b[2] = (byte) ((0xff0000 & i) >> 16);
		b[3] = (byte) ((0xff000000 & i) >> 24);
		return b;
	}

	public static byte[] long2bytes(long num) {
		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			b[i] = (byte) (num >>> (56 - (i * 8)));
		}
		return b;
	}

	public static short short2bytes(short s) {
		byte[] bs = new byte[2];
		for (int i = 0; i < 2; i++) {
			int offset = (bs.length - 1 - i) * 8;
			bs[i] = (byte) ((s >>> offset) & 0xff);
		}
		ByteBuffer buffer = ByteBuffer.wrap(bs);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getShort();
	}

	public static int bytes2int(byte[] bytes) {
		int num = bytes[0] & 0xFF;
		num |= ((bytes[1] << 8) & 0xFF00);
		num |= ((bytes[2] << 16) & 0xFF0000);
		num |= ((bytes[3] << 24) & 0xFF000000);
		return num;
	}

	public static long bytes2long(byte[] b) {
		long temp = 0;
		long res = 0;
		for (int i = 0; i < 8; i++) {
			res <<= 8;
			temp = b[i] & 0xff;
			res |= temp;
		}
		return res;
	}

	public static byte[] bytes2short(short s) {
		byte[] shortBuf = new byte[2];
		for (int i = 0; i < 2; i++) {
			int offset = (shortBuf.length - 1 - i) * 8;
			shortBuf[i] = (byte) ((s >>> offset) & 0xff);
		}
		return shortBuf;
	}
}

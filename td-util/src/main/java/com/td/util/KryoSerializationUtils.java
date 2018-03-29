package com.td.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang.SerializationException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * 高效序列化工具
 * 
 * @author wesley
 *
 */
public class KryoSerializationUtils {
	public final static Kryo kryo = new Kryo();

	static {
		kryo.setRegistrationRequired(false);
		kryo.setMaxDepth(Integer.MAX_VALUE);
	}

	public static byte[] serialize(Object obj) {
		ByteArrayOutputStream out = null;
		Output output = null;
		try {
			out = new ByteArrayOutputStream();
			output = new Output(out, 1024);
			kryo.writeClassAndObject(output, obj);
			return output.toBytes();
		} catch (Exception e) {
			throw new SerializationException(e);
		} finally {
			if (null != out) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
				}
			}
			if (null != output) {
				output.close();
				output = null;
			}
		}
	}

	public static Object deserialize(byte[] bytes) {
		Input input = null;
		try {
			input = new Input(bytes, 0, 1024);
			return kryo.readClassAndObject(input);
		} catch (Exception e) {
			throw new SerializationException(e);
		} finally {
			if (null != input) {
				input.close();
				input = null;
			}
		}
	}

	public static void main(String[] args) {
		String str = "我是一条测试";
		byte[] x = serialize(str);
		System.out.println(x);
		String temp = (String) deserialize(x);
		System.out.println(temp);
	}
}

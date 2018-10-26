package com.jbm.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationUtils;

public class Base64Utils {

	/**
	 * 将二进制数据编码为BASE64字符串
	 * 
	 * @param binaryData
	 * @return
	 */
	public static String encode(byte[] binaryData) {
		try {
			return new String(Base64.encodeBase64(binaryData), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * 将BASE64字符串恢复为二进制数据
	 * 
	 * @param base64String
	 * @return
	 */
	public static byte[] decode(String base64String) {
		try {
			return Base64.decodeBase64(base64String.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * 
	 * 将可序列化对象转换为字符串保存
	 * 
	 * @param obj
	 * @return
	 */
	public static String encodeSerializable(Serializable obj) {
		byte[] binaryData = null;
		if (obj == null)
			return null;
		if (obj instanceof String) {
			try {
				binaryData = Base64.decodeBase64(obj.toString().getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
			}
		}
		binaryData = SerializationUtils.serialize(obj);
		return Base64.encodeBase64String(binaryData);
	}

	public static Object decodeSerializable(String base64String) {
		return SerializationUtils.deserialize(Base64.decodeBase64(base64String));
	}

	public static Object decodeSerializable(byte[] base64Data) {
		return SerializationUtils.deserialize(Base64.decodeBase64(base64Data));
	}

	public static void main(String[] args) throws IOException {
		String testStr = "dsfasdfasdfasdczxc爱笑的发的是";
		System.out.println("test:" + testStr);
		String base64 = Base64Utils.encodeSerializable(testStr);
		System.out.println("base64:" + base64);
		System.out.println("test2:" + Base64Utils.decodeSerializable(base64));
	}

}

package com.td.util.encryp;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.td.util.Base64Utils;

public class AesUtils {

	private final static String DEF_KEY = "AES";

	private final static String DEF_PASSWORD = "wesley";

	private AesUtils() {

	}

	/**
	 * 对字符串加密
	 * 
	 * @param str
	 * @return
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] Encrytor(String str) {
		return Encrytor(str, DEF_PASSWORD);
	}

	public static byte[] Encrytor(String str, String password) {
		// 根据密钥，对Cipher对象进行初始化，ENCRYPT_MODE表示加密模式
		byte[] cipherByte = new byte[0];
		try {
			// 用户自定义密钥
			KeyGenerator keygen = KeyGenerator.getInstance(DEF_KEY);
			keygen.init(128, new SecureRandom(password.getBytes()));
			// 生成密钥
			SecretKey deskey = keygen.generateKey();
			Cipher cipher = Cipher.getInstance(DEF_KEY);
			cipher.init(Cipher.ENCRYPT_MODE, deskey);
			byte[] src = str.getBytes("utf-8");
			// 加密，结果保存进cipherByte
			cipherByte = cipher.doFinal(src);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return cipherByte;
	}

	public static String EncrytorToString(String str) {
		return Base64Utils.encode(Encrytor(str, DEF_PASSWORD));
	}

	public static String EncrytorToString(String str, String password) {
		return Base64Utils.encode(Encrytor(str, password));
	}

	public static byte[] Decryptor(byte[] buff) {
		return Decryptor(buff, DEF_PASSWORD);
	}

	public static String Decryptor(String base64String) {
		return Base64Utils.encode(Decryptor(Base64Utils.decode(base64String), DEF_PASSWORD));
	}

	public static String Decryptor(String base64String, String password) {
		return Base64Utils.encode(Decryptor(Base64Utils.decode(base64String), password));
	}

	/**
	 * 对字符串解密
	 * 
	 * @param buff
	 * @return
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static byte[] Decryptor(byte[] buff, String password) {
		// 根据密钥，对Cipher对象进行初始化，DECRYPT_MODE表示加密模式
		byte[] cipherByte = new byte[0];
		try {
			// 用户自定义密钥
			KeyGenerator keygen = KeyGenerator.getInstance(DEF_KEY);
			keygen.init(128, new SecureRandom(password.getBytes()));
			// 生成密钥
			SecretKey deskey = keygen.generateKey();
			Cipher cipher = Cipher.getInstance(DEF_KEY);
			cipher.init(Cipher.DECRYPT_MODE, deskey);

			cipherByte = cipher.doFinal(buff);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
		return cipherByte;
	}

	/**
	 * @param args
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	public static void main(String[] args) throws Exception {
		String msg = "郭XX-搞笑相声全集";
		String miwen = AesUtils.EncrytorToString(msg);
		String minwen = AesUtils.Decryptor(miwen);
		System.out.println("明文是:" + msg);
		System.out.println("加密后:" + miwen);
		System.out.println("解密后:" + minwen);
	}

}
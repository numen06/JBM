package com.td.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.SerializationUtils;

/**
 * 
 * 压缩工具类
 * 
 * @author Wesley
 * 
 */
public class GZipUtils {

	public static final int BUFFER = 1024;
	public static final String ZIP_EXT = ".gz";

	/**
	 * 数据压缩序列化对象
	 * 
	 * @param serial
	 * @return
	 * @throws IOException
	 */
	public static byte[] compress(Serializable serial) throws IOException {
		byte[] bytes = SerializationUtils.serialize(serial);
		return compress(bytes);
	}

	/**
	 * 数据压缩
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// 压缩
		compress(bais, baos);

		byte[] output = baos.toByteArray();

		baos.flush();
		baos.close();

		bais.close();

		return output;
	}

	/**
	 * 文件压缩
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void compress(File file) throws IOException {
		compress(file, true);
	}

	/**
	 * 文件压缩
	 * 
	 * @param file
	 * @param delete
	 *            是否删除原始文件
	 * @throws IOException
	 * @throws IOException
	 */
	public static void compress(File file, boolean delete) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(file.getPath() + ZIP_EXT);

		compress(fis, fos);

		fis.close();
		fos.flush();
		fos.close();

		if (delete) {
			file.delete();
		}
	}

	/**
	 * 数据压缩
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 * @throws IOException
	 */
	public static void compress(InputStream is, OutputStream os) throws IOException {
		GZIPOutputStream gos = new GZIPOutputStream(os);
		int count;
		byte data[] = new byte[BUFFER];
		while ((count = is.read(data, 0, BUFFER)) != -1) {
			gos.write(data, 0, count);
		}

		gos.finish();
		gos.flush();
		gos.close();

	}

	/**
	 * 文件压缩
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void compress(String path) throws IOException {
		compress(path, true);
	}

	/**
	 * 文件压缩
	 * 
	 * @param path
	 * @param delete
	 *            是否删除原始文件
	 * @throws IOException
	 */
	public static void compress(String path, boolean delete) throws IOException {
		File file = new File(path);
		compress(file, delete);
	}

	/**
	 * 数据解压缩成一个对象
	 * 
	 * @param data
	 * @param obj
	 * @throws IOException
	 */
	public static void decompress(byte[] data, Object obj) throws IOException {
		obj = SerializationUtils.deserialize(decompress(data));
	}

	/**
	 * 数据解压缩
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static byte[] decompress(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 解压缩
		decompress(bais, baos);
		data = baos.toByteArray();
		baos.flush();
		baos.close();
		bais.close();
		return data;
	}

	/**
	 * 文件解压缩
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void decompress(File file) throws IOException {
		decompress(file, true);
	}

	/**
	 * 文件解压缩
	 * 
	 * @param file
	 * @param delete
	 *            是否删除原始文件
	 * @throws IOException
	 */
	public static void decompress(File file, boolean delete) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(file.getPath().replace(ZIP_EXT, ""));
		decompress(fis, fos);
		fis.close();
		fos.flush();
		fos.close();

		if (delete) {
			file.delete();
		}
	}

	/**
	 * 数据解压缩
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void decompress(InputStream is, OutputStream os) throws IOException {

		GZIPInputStream gis = new GZIPInputStream(is);

		int count;
		byte data[] = new byte[BUFFER];
		while ((count = gis.read(data, 0, BUFFER)) != -1) {
			os.write(data, 0, count);
		}

		gis.close();
	}

	/**
	 * 文件解压缩
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void decompress(String path) throws IOException {
		decompress(path, true);
	}

	/**
	 * 文件解压缩
	 * 
	 * @param path
	 * @param delete
	 *            是否删除原始文件
	 * @throws IOException
	 */
	public static void decompress(String path, boolean delete) throws IOException {
		File file = new File(path);
		decompress(file, delete);
	}

	private static String inputStr = "zlex@zlex.org,snowolf@zlex.org,zlex.snowolf@zlex.org";

	public static void main(String[] args) throws IOException {
		System.err.println("原文:\t" + inputStr);

		byte[] input = inputStr.getBytes();
		System.err.println("长度:\t" + input.length);

		byte[] data = GZipUtils.compress(input);
		System.err.println("压缩后:\t");
		System.err.println("长度:\t" + data.length);

		byte[] output = GZipUtils.decompress(data);
		String outputStr = new String(output);
		System.err.println("解压缩后:\t" + outputStr);
		System.err.println("长度:\t" + output.length);

		// assertEquals(inputStr, outputStr);
	}

}
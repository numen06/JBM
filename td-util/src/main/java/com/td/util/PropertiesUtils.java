package com.td.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Maps;

import jodd.util.ClassLoaderUtil;

public class PropertiesUtils {

	/**
	 * 读取文件
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Properties load(File file) throws IOException {
		Properties pros = new Properties();
		InputStream in = FileUtils.openInputStream(file);
		pros.load(in);
		in.close();
		return pros;
	}

	/**
	 * 读取资源文件夹里的配置文件
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Properties loadClassPath(String path) throws IOException {
		Properties pros = new Properties();
		InputStream in = FileUtils.openInputStream(ClassLoaderUtil.getResourceFile(path));
		pros.load(in);
		in.close();
		return pros;
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> loadToMap(File file) {
		Map<String, String> result = Maps.newHashMap();
		Properties pros;
		try {
			pros = PropertiesUtils.load(file);
		} catch (IOException e) {
			return result;
		}
		result = Maps.fromProperties(pros);
		if (MapUtils.isEmpty(result))
			return Maps.newHashMap();
		return Maps.newHashMap(result);
	}

	public static void save(File file, Properties properties) throws IOException {
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream oFile = FileUtils.openOutputStream(file);
		properties.store(oFile, null);
		oFile.close();
	}

	public static <V extends Object> void save(File file, Map<String, V> map) throws IOException {
		if (!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream oFile = FileUtils.openOutputStream(file);
		Properties prop = new Properties();
		Set<Entry<String, V>> entries = map.entrySet();
		for (Iterator<Entry<String, V>> iterator = entries.iterator(); iterator.hasNext();) {
			Entry<String, V> entry = iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			prop.setProperty(key, value);
			prop.store(oFile, null);
		}
		oFile.close();
	}
}

package com.jbm.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.jbm.util.ini.IniReader;

import jodd.util.ClassLoaderUtil;

/**
 * 对ini文件进行操作
 * 
 * @author wesley
 *
 */
public class IniUtils {

	/**
	 * 读取配置文件下面的ini文件
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static IniReader loadClassPath(String path) throws IOException {
		InputStream file = ClassLoaderUtil.getResourceAsStream(path);
		return new IniReader(file);
	}

	/**
	 * 读取ini文件
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static IniReader load(File file) throws IOException {
		return new IniReader(file);
	}

}

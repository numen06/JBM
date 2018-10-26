package com.jbm.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jbm.util.bean.ClassPathFile;

import jodd.io.FileNameUtil;

/**
 * 
 * 对于文件进行操作的工具类
 * 
 * @author Wesley
 * 
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

	/**
	 * 判断文件是否存在
	 * 
	 * @param file
	 * @return
	 */
	public static boolean exists(File file) {
		return file.exists();
	}

	/**
	 * 判断多个文件是否存在
	 * 
	 * @param filePath
	 * @return
	 */
	public static boolean exists(String... filePath) {
		for (int i = 0; i < filePath.length; i++) {
			if (!exists(new File(filePath[i])))
				return false;
		}
		return true;
	}

	/**
	 * 获取多个文件
	 * 
	 * @param filePath
	 * @return
	 */
	public static File[] getFiles(String... filePath) {
		File[] files = new File[filePath.length];
		for (int i = 0; i < filePath.length; i++) {
			files[i] = new File(filePath[i]);
		}
		return files;
	}

	/**
	 * 连续创建文件夹
	 * 
	 * @param path
	 * @throws IOException
	 */
	public static void forceMkdir(String path) throws IOException {
		FileUtils.forceMkdir(new File(path));
	}

	public static File getClassPathFile(String path) throws IOException {
		return new ClassPathFile(path).getFile();
	}

	public static void main(String[] args) throws IOException {
		FileUtils.forceMkdir("d:/test/test/test");
		System.out.println(FileUtils.exists("d:/test/test/test"));
		System.out.println(FileUtils.getClassPathFile("test.txt"));
	}

	public static File[] filesMatchingStemRegex(File file, final String stemRegex) {
		return filesMatchingStemRegex(file, new ArrayList<File>(), "/", stemRegex);
	}

	public static File[] filesMatchingStemRegex(File file, List<File> result, String prefix, final String stemRegex) {
		if (file == null) {
			return new File[0];
		}
		if (!file.exists() || !file.isDirectory()) {
			return new File[0];
		}
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			File path = files[i];
			if (file.exists() && file.isDirectory()) {
				filesMatchingStemRegex(path, result, FileNameUtil.concat(prefix, path.getName(), true), stemRegex);
			}
		}
		File[] temp = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String tempName = FileNameUtil.concat(prefix, name, true);
				return tempName.matches(stemRegex);
			}
		});
		CollectionUtils.addAll(result, temp);
		return result.toArray(new File[0]);
	}

}

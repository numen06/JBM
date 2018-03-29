package com.td.test.devops;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;

import com.td.util.StringUtils;

import junit.framework.TestCase;

public class ProcessTest extends TestCase {

	public void testMD5() {
	}

	public static String fileMD5(String inputFile) throws IOException {
		// 缓冲区大小（这个可以抽出一个参数）
		int bufferSize = 256 * 1024;
		FileInputStream fileInputStream = null;
		DigestInputStream digestInputStream = null;
		try {
			// 拿到一个MD5转换器（同样，这里可以换成SHA1）
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			// 使用DigestInputStream
			fileInputStream = new FileInputStream(inputFile);
			digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
			// read的过程中进行MD5处理，直到读完文件
			byte[] buffer = new byte[bufferSize];
			while (digestInputStream.read(buffer) > 0)
				;
			// 获取最终的MessageDigest
			messageDigest = digestInputStream.getMessageDigest();
			// 拿到结果，也是字节数组，包含16个元素
			byte[] resultByteArray = messageDigest.digest();
			// 同样，把字节数组转换成字符串
			return byteArrayToHex(resultByteArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		} finally {
			try {
				digestInputStream.close();
			} catch (Exception e) {
			}
			try {
				fileInputStream.close();
			} catch (Exception e) {
			}
		}
	}

	public static String byteArrayToHex(byte[] byteArray) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < byteArray.length; n++) {
			stmp = (Integer.toHexString(byteArray[n] & 0XFF));
			if (stmp.length() == 1) {
				hs = hs + "0" + stmp;
			} else {
				hs = hs + stmp;
			}
			if (n < byteArray.length - 1) {
				hs = hs + "";
			}
		}
		return hs;

	}

	public void testFindProcess() throws IOException {
		Process process = Runtime.getRuntime().exec("cmd /c tasklist |findstr 1700");
		System.out.println(IOUtils.readLines(process.getInputStream()).isEmpty());
		System.out.println(StringUtils.join(IOUtils.readLines(process.getInputStream()), "\n"));
	}

	public void testkillProcess() throws IOException {
		Process process = Runtime.getRuntime().exec("cmd /c taskkill /PID 1700 /F /T");
		System.out.println(IOUtils.readLines(process.getInputStream()).isEmpty());
		System.out.println(StringUtils.join(IOUtils.readLines(process.getInputStream()), "\n"));
	}

	public void testRunJar() throws IOException {
		Process process = Runtime.getRuntime().exec("cmd /c cd sources && start.bat");
		System.out.println(StringUtils.join(IOUtils.readLines(process.getInputStream()), "\n"));
	}

	public void testRunJar2() throws IOException {
		ProcessBuilder process = new ProcessBuilder("cmd", "/c", "cd sources && start.bat");
		process.redirectError(new File("error.txt"));
		process.redirectOutput(new File("out.txt"));
		process.start();
	}

}

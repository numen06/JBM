package com.jbm.util.encryp;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * MD5加密工具类
 *
 * @author Wesley
 */
@Slf4j
public class MD5Utils {

    private static final Logger logger = LoggerFactory.getLogger(MD5Utils.class);

    protected static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    protected static MessageDigest messagedigest = null;

    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5FileUtil messagedigest初始化失败", e);
        }
    }

    /**
     * 将字符串转成MD5加密字符串
     *
     * @param source 输入字符串
     * @return 加密后的MD5字符串
     */
    public static String MD5(String source) {
        return MD5(source.getBytes()).toUpperCase();
    }

    /**
     * 多个字符串拼接成一个MD5
     *
     * @param sources
     * @return
     */
    public static String MD5(String... sources) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sources.length; i++) {
            sb.append(sources[i]);
        }
        return MD5(sb.toString().getBytes()).toUpperCase();
    }

    /**
     * 将二进制数据通过MD5加密
     *
     * @param bytes 输入字节流
     * @return 加密后的MD5字符串
     */
    public static String MD5(byte[] bytes) {
        messagedigest.update(bytes);
        return bufferToHex(messagedigest.digest());
    }

    private static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];
        char c1 = hexDigits[bt & 0xf];
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }

    /**
     * 比较字符串和MD5字符串之间是否匹配
     *
     * @param source 输入字符串
     * @param md5Str 加密字符串
     * @return 比较结果
     */
    public static boolean compareTo(String source, String md5Str) {
        String s = MD5(source);
        return s.equalsIgnoreCase(md5Str);
    }

    public static void main(String[] args) throws IOException {
        long begin = System.currentTimeMillis();

        String md5 = MD5("D:\\temp\\jre-7u11-linux-i586.tar.gz");

        long end = System.currentTimeMillis();
        System.out.println("md5:" + md5);
        System.out.println("md5_2:" + MD5(begin + ""));
        System.out.println("time:" + ((end - begin) / 1000) + "s");

    }
}

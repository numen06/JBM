package com.jbm.cluster.doc.common.file;

import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class WebFileUtil {

    // office
    private static String[] office = {"word", "excel", "ppt"};

    // excel
    private static String[] etExts = {"et", "xls", "xlt", "xlsx", "xlsm", "xltx", "xltm", "csv"};

    // word
    private static String[] wpsExts = {"doc", "docx", "txt", "dot", "wps", "wpt", "dotx", "docm", "dotm"};

    // ppt
    private static String[] wppExts = {"ppt", "pptx", "pptm", "pptm", "ppsm", "pps", "potx", "potm", "dpt", "dps"};

    // pdf
    private static String[] pdfExts = {"pdf"};

    public static String getFileTypeCode(String fileType) {
        for (String et : etExts) {
            if (et.equalsIgnoreCase(fileType)) {
                return "s";
            }
        }
        for (String et : wpsExts) {
            if (et.equalsIgnoreCase(fileType)) {
                return "w";
            }
        }
        for (String et : wppExts) {
            if (et.equalsIgnoreCase(fileType)) {
                return "p";
            }
        }
        for (String et : pdfExts) {
            if (et.equalsIgnoreCase(fileType)) {
                return "f";
            }
        }
        return null;
    }

    public static boolean checkCode(String fileType) {
        for (String et : office) {
            if (et.equalsIgnoreCase(fileType)) {
                return true;
            }
        }
        return false;
    }

    public static String getTypeCode(String fileType) {
        if ("word".equalsIgnoreCase(fileType)) {
            return "w";
        }
        if ("excel".equalsIgnoreCase(fileType)) {
            return "s";
        }
        if ("ppt".equalsIgnoreCase(fileType)) {
            return "p";
        }
        return null;
    }

    public static String getFileName(String filePath) {
        String[] pathArr = filePath.split("/");
        String fileName;
        if (pathArr.length > 1) {
            fileName = pathArr[pathArr.length - 1];
        } else {
            fileName = filePath;
        }
        return fileName;
    }

    public static String getFileTypeByPath(String filePath) {
        String fileName = getFileName(filePath);
        String[] arr = fileName.split("\\.");
        return arr[arr.length - 1];
    }

    public static String getFileTypeByName(String fileName) {
        String[] arr = fileName.split("\\.");
        return arr[arr.length - 1];
    }

    public static String getFileUUIDName(String fileName, String fileType) {
        String uuid = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String uuidFileName = fileName.replace(".", "").replace(fileType, "") + uuid + "." + fileType;
        return new String(uuidFileName.getBytes(), StandardCharsets.UTF_8);
    }

    public static String makeNewFileName(String oldFileName) {
        String fileType = WebFileUtil.getFileTypeByName(oldFileName);
        String tempFileName = oldFileName.replace("." + fileType, "");
        Random ne = new Random();//实例化一个random的对象ne
        int uuid = ne.nextInt(90000) + 10000;//为变量赋随机值10000-99999
        return tempFileName + uuid + "." + fileType;
    }

    public static String makeNewFileName(String oldFileName, String fileType) {
        String tempFileName = oldFileName.replace("." + fileType, "");
        Random ne = new Random();//实例化一个random的对象ne
        int uuid = ne.nextInt(90000) + 10000;//为变量赋随机值10000-99999
        return tempFileName + uuid + "." + fileType;
    }

    public static int getFileSize(String fileUrl) {
        if (fileUrl == null || "".equals(fileUrl)) {
            return 0;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            return conn.getContentLength();
        } catch (Exception e) {
            return 0;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // 这里cn 或者 com 根据自己域名适当调整下哟
    // TODO
    public static String subFileUrl(String url) {
        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(0, index);
        }

        int comIndex = url.indexOf(".cn/");
        if (comIndex != -1) {
            url = StringUtils.substringAfter(url, ".cn/");
        }

        return url;
    }

}

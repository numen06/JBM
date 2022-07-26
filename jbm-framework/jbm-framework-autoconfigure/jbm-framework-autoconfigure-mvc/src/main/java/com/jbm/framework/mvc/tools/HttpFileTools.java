package com.jbm.framework.mvc.tools;

import com.google.common.collect.Lists;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HttpFileTools {

    /**
     * 返回下载文件
     *
     * @param response
     * @param fileName
     * @param fileBytes
     * @throws IOException
     */
    public static void downlaodFile(final HttpServletResponse response, final String fileName, final byte[] fileBytes)
            throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);
        try {
            response.setHeader("Content-Disposition",
                    "inline;fileName=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(fileBytes.length);
        OutputStream stream;
        stream = response.getOutputStream();
        stream.write(fileBytes);
        stream.flush();
        stream.close();
    }

    /**
     * 上传文件
     *
     * @param request
     * @return
     */
    public static List<MultipartFile> uploadFiles(HttpServletRequest request) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        Collection<MultipartFile> files = fileMap.values();
        return Lists.newArrayList(files);
    }

}

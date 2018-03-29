package com.td.framework.mvc.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.google.common.collect.Lists;

public class HttpFileTools {

	public static void downlaodFile(final HttpServletResponse response, final String fileName, final byte[] fileBytes) throws IOException {
		String mimeType = URLConnection.guessContentTypeFromName(fileName);
		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}
		response.setContentType(mimeType);
		try {
			response.setHeader("Content-Disposition", "inline;fileName=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
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

	public static List<MultipartFile> uploadFiles(HttpServletRequest request) {
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		Collection<MultipartFile> files = fileMap.values();
		return Lists.newArrayList(files);
	}

}

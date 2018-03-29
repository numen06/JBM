package com.td.web.tools;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.td.framework.metadata.usage.bean.FileInfoBean;

/**
 * 下载工具类
 * 
 * @author wesley
 *
 */
public class DownloadTools {

	/**
	 * 下载文件
	 * 
	 * @param response
	 * @param fileInfo
	 * @throws IOException
	 */
	public static void downlaodFile(HttpServletResponse response, FileInfoBean fileInfo) throws IOException {
		String fileName = new String(fileInfo.getFileName().getBytes("UTF-8"), "ISO8859-1");
		Long fileLength = fileInfo.getLength();
		BufferedOutputStream outputStream = null;
		try {
			// 重置
			response.reset();
			// 设置文件输出类型
			if (StringUtils.isBlank(fileInfo.getContentType()))
				response.setContentType("application/x-msdownload;charset=UTF-8");
			else
				response.setContentType(fileInfo.getContentType());
			response.setHeader("content-disposition", "attachment;filename=" + fileName);
			response.setHeader("content-length", String.valueOf(fileLength));
			outputStream = new BufferedOutputStream(response.getOutputStream());
			outputStream.write(fileInfo.getFileBytes());
			outputStream.flush();
		} finally {
			outputStream.close();
		}
	}
}

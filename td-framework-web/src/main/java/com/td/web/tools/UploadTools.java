package com.td.web.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import com.td.framework.metadata.usage.bean.FileInfoBean;

/**
 * 上传工具类
 * 
 * @author wesley
 *
 */
public class UploadTools {

	/**
	 * 从请求中获取文件
	 * 
	 * @param multipartRequest
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public static FileInfoBean getFileFormRequet(MultipartRequest multipartRequest, String name) throws IOException {
		return buildFileInfoBean(multipartRequest.getFile(name));
	}

	/**
	 * 从请求中获取多个文件
	 * 
	 * @param multipartRequest
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public static List<FileInfoBean> getFilesFormRequet(MultipartRequest multipartRequest, String name) throws IOException {
		return buildFileInfoBean(multipartRequest.getFiles(name));
	}

	/**
	 * 通过上传文件得到文件信息
	 * 
	 * @param multipartFile
	 * @return
	 * @throws IOException
	 */
	public static FileInfoBean buildFileInfoBean(MultipartFile multipartFile) throws IOException {
		FileInfoBean fileInfo = new FileInfoBean(multipartFile.getOriginalFilename(), null, multipartFile.getOriginalFilename(), multipartFile.getBytes());
		return fileInfo;
	}

	/**
	 * 通过多个上传文件得到文件信息
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static List<FileInfoBean> buildFileInfoBean(List<MultipartFile> file) throws IOException {
		List<FileInfoBean> files = new ArrayList<FileInfoBean>();
		for (Iterator<MultipartFile> iterator = file.iterator(); iterator.hasNext();) {
			MultipartFile multipartFile = iterator.next();
			files.add(buildFileInfoBean(multipartFile));
		}
		return files;
	}
}

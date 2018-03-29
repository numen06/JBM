package com.td.framework.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.mongodb.gridfs.GridFSDBFile;
import com.td.framework.metadata.usage.bean.FileInfoBean;

public interface FileSupportService {

	public InputStream getInputStream(String fileHandler);

	public FileInfoBean saveFile(File file) throws IOException;

	public FileInfoBean saveFile(String filePath) throws IOException;

	public Long writeTo(String fileHandler, OutputStream outputStream) throws IOException;

	public FileInfoBean saveByteToFile(byte[] bytes) throws IOException;

	public FileInfoBean saveStringToFile(String str) throws IOException;

	public FileInfoBean saveByteToFile(byte[] bytes, String fileHandler) throws IOException;

	public GridFSDBFile getFile(String fileHandler);

	public FileInfoBean saveByteToFile(byte[] bytes, String saveName, String contentType) throws IOException;

	public GridFSDBFile getFileById(String fileId);

	public FileInfoBean saveStringToFile(String str, String saveName, String contentType) throws IOException;

	public GridFSDBFile getFile(FileInfoBean fileInfoBean);

	public Boolean exits(FileInfoBean fileInfoBean);

	public Long writeTo(FileInfoBean fileInfoBean, OutputStream outputStream) throws IOException;

	public String writeToString(FileInfoBean fileInfoBean) throws IOException;

	public byte[] writeTo(FileInfoBean fileInfoBean) throws IOException;

	public String writeToBase64String(FileInfoBean fileInfoBean) throws IOException;

	public FileInfoBean saveByteToFile(FileInfoBean fileInfoBean) throws IOException;

	public FileInfoBean writeToFileInfoBean(String fileId) throws IOException;

	public FileInfoBean writeToFileInfoBean(FileInfoBean fileInfoBean) throws IOException;

	public Long delete(FileInfoBean fileInfoBean);

	public Long deleteById(String fileId);

	/**
	 * 完成保存FileInfoBean中的信息
	 * 
	 * @param fileInfoBean
	 * @return
	 * @throws IOException
	 */
	FileInfoBean saveFile(FileInfoBean fileInfoBean) throws IOException;

}

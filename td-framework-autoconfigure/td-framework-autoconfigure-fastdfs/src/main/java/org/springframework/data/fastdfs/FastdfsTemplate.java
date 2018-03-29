package org.springframework.data.fastdfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.util.StringUtils;

import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.bean.FileInfoBean;

import jodd.io.FileNameUtil;
import jodd.util.MimeTypes;
import jodd.util.StringUtil;

/**
 * @author wesley.zhang
 *
 */
public class FastdfsTemplate {
	private StorageClient1 storageClient = null;
	private GenericObjectPool<StorageClient1> storageClients = new GenericObjectPool<StorageClient1>(new BasePooledObjectFactory<StorageClient1>() {
		private StorageClient1 makeStorageClient() throws IOException {
			TrackerClient trackerClient = new TrackerClient();
			TrackerServer trackerServer = trackerClient.getConnection();
			StorageServer storageServer = null;
			return storageClient = new StorageClient1(trackerServer, storageServer);
		}

		@Override
		public StorageClient1 create() throws Exception {
			return makeStorageClient();
		}

		@Override
		public PooledObject<StorageClient1> wrap(StorageClient1 obj) {
			return new DefaultPooledObject<StorageClient1>(obj);
		}
	});

	public FastdfsTemplate(String trackerServers) throws Exception {
		ClientGlobal.initByTrackers(trackerServers);
		PoolUtils.prefill(storageClients, 10);
		storageClient = storageClients.borrowObject();
	}

	public String uploadFile(final String fileName, final String extName, NameValuePair[] metas) throws Exception {
		String result = storageClient.upload_file1(fileName, extName, metas);
		return result;
	}

	public String uploadFile(final String fileName) throws Exception {
		String extName = StringUtils.getFilenameExtension(fileName);
		String result = storageClient.upload_file1(fileName, extName, null);
		return result;
	}

	public String uploadFile(final String fileName, final String extName) throws Exception {
		String result = storageClient.upload_file1(fileName, extName, null);
		return result;
	}

	public byte[] downloadFile(final String filePath) throws Exception {
		byte[] result = storageClient.download_file1(filePath);
		return result;
	}

	public int deleteFile(final String fileId) throws Exception {
		int result = storageClient.delete_file1(fileId);
		return result;
	}

	public FileInfo getFileInfo(final String fileId) throws Exception {
		FileInfo result = storageClient.get_file_info1(fileId);
		return result;
	}

	public FileInfo queryFileInfo(final String fileId) throws Exception {
		FileInfo result = storageClient.query_file_info1(fileId);
		return result;
	}

	public NameValuePair[] getMetadata(final String fileId) throws Exception {
		NameValuePair[] result = storageClient.get_metadata1(fileId);
		return result;
	}

	public String pathToId(String path) {
		return FileNameUtil.getBaseName(StringUtils.getFilename(path));
	}

	// private NameValuePair[] mapToNameValuePair(FileInfoBean fileInfoBean) {
	// NameValuePair[] arr = new NameValuePair[3];
	// fileInfoBean.setContentType(MimeTypes.lookupMimeType(fileInfoBean.getFileName()));
	// arr[0] = new NameValuePair("ContentType", fileInfoBean.getContentType());
	// arr[1] = new NameValuePair("MetaData",
	// JSON.toJSONString(fileInfoBean.getMetaData()));
	// arr[2] = new NameValuePair("MD5",
	// MD5.asHex(fileInfoBean.getFileBytes()));
	// // int i = 0;
	// // for (String key : fileInfoBean.getMetaData().keySet()) {
	// // arr[i] = new NameValuePair(key,
	// // JSON.toJSONString(fileInfoBean.getMetaData().get(key)));
	// // }
	// return arr;
	// }

	public FileInfoBean updateFileInfoBean(FileInfoBean fileInfoBean) throws Exception {
		String dfspath = null;
		Path localPath = null;
		StorageClient1 storageClient1 = storageClients.borrowObject();
		synchronized (storageClient1) {
			try {
				boolean hasFile = false;
				if (StringUtil.isNotBlank(fileInfoBean.getFilePath())) {
					FileInfo fileInfo = storageClient1.get_file_info1(fileInfoBean.getFilePath());
					if (fileInfo != null) {
						hasFile = true;
					}
				}
				if (!hasFile) {
					if (StringUtil.isBlank(fileInfoBean.getExtension())) {
						fileInfoBean.setExtension(FileNameUtil.getExtension(fileInfoBean.getFileName()));
					}
					if (StringUtil.isBlank(fileInfoBean.getExtension())) {
						fileInfoBean.setExtension("bin");
					}
					if (StringUtil.isBlank(fileInfoBean.getContentType()))
						fileInfoBean.setContentType(MimeTypes.lookupMimeType(fileInfoBean.getExtension()));
					if (fileInfoBean.getLocalFilePath() != null) {
						localPath = Paths.get(fileInfoBean.getLocalFilePath());
						if (Files.exists(localPath)) {
							if (Files.size(localPath) <= 0)
								throw new IOException("文件为空");
							dfspath = storageClient1.upload_appender_file1(localPath.toFile().toString(), fileInfoBean.getExtension(), null);
						}
					} else {
						if (fileInfoBean.getFileBytes() == null || fileInfoBean.getFileBytes().length <= 0)
							throw new IOException("字节流为空");
						dfspath = storageClient1.upload_file1(fileInfoBean.getFileBytes(), fileInfoBean.getExtension(), null);
					}
					if (dfspath == null) {
						throw new IOException("远程服务器错误");
					}
				}
				fileInfoBean.setFilePath(dfspath);
				if (fileInfoBean.getId() == null)
					fileInfoBean.setId(this.pathToId(dfspath));
				FileInfo fileInfo = storageClient1.get_file_info1(dfspath);
				fileInfoBean.setLength(fileInfo.getFileSize());
				fileInfoBean.setUploadDate(fileInfo.getCreateTimestamp());
			} catch (Exception e) {
				throw new IOException("上传DFS文件错误", e);
			} finally {
				if (localPath != null)
					Files.deleteIfExists(localPath);
				storageClients.returnObject(storageClient);
			}
		}
		return fileInfoBean;
	}

	public FileInfoBean downloadFileInfoBean(FileInfoBean fileInfoBean) throws Exception {
		StorageClient1 storageClient1 = storageClients.borrowObject();
		synchronized (storageClient1) {
			FileInfo fileInfo = null;
			if (fileInfoBean.getFilePath() == null) {
				throw new ServiceException("file not found");
			}
			fileInfo = storageClient1.get_file_info1(fileInfoBean.getFilePath());
			// NameValuePair[] nameValuePairs =
			// storageClient.get_metadata1(path);
			if (fileInfo == null) {
				throw new ServiceException("dfs file not found");
			}
			Path tempFile = null;
			try {
				tempFile = Files.createTempFile("dfs", ".temp");
				storageClient1.download_file1(fileInfoBean.getFilePath(), tempFile.toFile().toString());
				// storageClient.download_file1(fileInfoBean.getFilePath(),
				// tempFile.toFile().toString());
				fileInfoBean.setFileBytes(Files.readAllBytes(tempFile));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (tempFile != null)
					Files.deleteIfExists(tempFile);
				storageClients.returnObject(storageClient);
			}
			// fileInfoBean.setFileName("wKgBalqU75KAEolDAADB6j3X12M607.jpg");
			if (fileInfoBean != null)
				fileInfoBean.setLength(new Long(fileInfoBean.getFileBytes().length));
			fileInfoBean.setUploadDate(fileInfo.getCreateTimestamp());
		}
		return fileInfoBean;
	}

	public FileInfoBean deleteFileInfoBean(FileInfoBean fileInfoBean) throws Exception {
		storageClient.delete_file1(fileInfoBean.getFilePath());
		return fileInfoBean;
	}

}

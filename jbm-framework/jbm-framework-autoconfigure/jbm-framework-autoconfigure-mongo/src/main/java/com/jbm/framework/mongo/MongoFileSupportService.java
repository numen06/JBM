package com.jbm.framework.mongo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.jbm.framework.metadata.usage.bean.FileInfoBean;
import com.jbm.util.Base64Utils;
import com.jbm.util.PathUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;

public class MongoFileSupportService implements FileSupportService {

	private static final Logger logger = LoggerFactory.getLogger(MongoFileSupportService.class);

	public MongoFileSupportService() {
		super();
	}

	public MongoFileSupportService(GridFsTemplate gridFsTemplate, GridFSBucket gridFSBucket) {
		super();
		this.gridFsTemplate = gridFsTemplate;
		this.gridFSBucket = gridFSBucket;
	}

	private GridFsTemplate gridFsTemplate;

	private GridFSBucket gridFSBucket;

	@Override
	public FileInfoBean saveByteToFile(FileInfoBean fileInfoBean) throws IOException {
		byte[] data = fileInfoBean.getFileBytes();
		return saveByteToFile(data, fileInfoBean.getFileName(), fileInfoBean.getContentType());
	}

	@Override
	public FileInfoBean saveByteToFile(byte[] bytes) throws IOException {
		return saveByteToFile(bytes, null, null);
	}

	@Override
	public FileInfoBean saveByteToFile(byte[] bytes, String saveName, String contentType) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		if (StringUtils.isBlank(saveName)) {
			saveName = PathUtils.randomFileName("txt");
		}
		FileInfoBean file = this.saveFile(in, saveName, contentType);
		return file;
	}

	@SuppressWarnings("unchecked")
	private FileInfoBean buildFileInfoBean(JbmGridFSDBFile file) {
		FileInfoBean fileBean = new FileInfoBean(file.getId().toString(), file.getFilename(), file.getContentType(),
				file.getLength(), file.getChunkSize(), file.getUploadDate(), file.getMD5(), null);
		if (file.getMetaData() != null) {
			fileBean.setMetaData(file.getMetaData());
			if (fileBean.getMetaData().containsKey("originalName")) {
				fileBean.setOriginalName(fileBean.getMetaData().get("originalName").toString());
			}
		}
		return fileBean;
	}

	@Override
	public FileInfoBean saveByteToFile(byte[] bytes, String fileHandler) throws IOException {
		InputStream in = new ByteArrayInputStream(bytes);
		return this.saveFile(in, fileHandler);
	}

	@Override
	public FileInfoBean saveStringToFile(String str) throws IOException {
		return saveByteToFile(str.getBytes("UTF-8"));
	}

	@Override
	public FileInfoBean saveStringToFile(String str, String saveName, String contentType) throws IOException {
		return saveByteToFile(str.getBytes("UTF-8"), saveName, contentType);
	}

	@Override
	public FileInfoBean saveFile(String filePath) throws IOException {
		return saveFile(filePath, null);
	}

	public FileInfoBean saveFile(String filePath, String contentType) throws IOException {
		File file = new File(filePath);
		return saveFile(file, file.getName(), contentType);
	}

	@Override
	public FileInfoBean saveFile(File file) throws IOException {
		return saveFile(file, file.getName());
	}

	public FileInfoBean saveFile(File file, String originalName) throws IOException {
		return saveFile(file, originalName, null);
	}

	public FileInfoBean saveFile(File file, String originalName, String contentType) throws IOException {
		FileInputStream inputStream = new FileInputStream(file);
		return saveFile(inputStream, originalName, contentType);
	}

	public FileInfoBean saveFile(InputStream inputStream, String originalName) throws IOException {
		return saveFile(inputStream, originalName, null);
	}

	public FileInfoBean saveFile(InputStream inputStream, String originalName, String contentType) throws IOException {
		return saveFile(false, inputStream, originalName, contentType);
	}

	@Override
	public FileInfoBean saveFile(FileInfoBean fileInfoBean) throws IOException {
		String saveName = fileInfoBean.getFileName();
		byte[] bb = fileInfoBean.getFileBytes();
		if (bb == null)
			throw new IOException("上传的文件为空");
		DBObject metadata = null;
		if (MapUtils.isNotEmpty(fileInfoBean.getMetaData()))
			metadata = new BasicDBObject(fileInfoBean.getMetaData());
		String contentType = fileInfoBean.getContentType();
		if (StringUtils.isEmpty(contentType)) {
			contentType = PathUtils.getStandardExtension(saveName);
		}
		InputStream inputStream = new ByteArrayInputStream(bb);
//		GridFSFile file = gridFsTemplate.store(inputStream, saveName, fileInfoBean.getContentType(), metadata);
		ObjectId file = gridFsTemplate.store(inputStream, saveName, fileInfoBean.getContentType(), metadata);
		return buildFileInfoBean(this.getFileById(file.toString()));
	}

	public FileInfoBean saveFile(boolean randomName, InputStream inputStream, String originalName, String contentType)
			throws IOException {
		String saveName = originalName;
		DBObject metadata = null;
		if (randomName) {
			saveName = PathUtils.randomFileName(originalName);
			metadata = new BasicDBObject("originalName", originalName);
		}
		if (StringUtils.isEmpty(contentType)) {
			contentType = PathUtils.getStandardExtension(originalName);
		}
		ObjectId file = gridFsTemplate.store(inputStream, saveName, contentType, metadata);
		return buildFileInfoBean(this.getFileById(file.toString()));
	}

	@Override
	public Boolean exits(FileInfoBean fileInfoBean) {
		JbmGridFSDBFile file = this.getFile(fileInfoBean);
		return (file != null);
	}

	@Override
	public Long delete(FileInfoBean fileInfoBean) {
		Query query = genQuery(fileInfoBean);
		// 如果查询都是空的那么取消删除
		if (query.getQueryObject().size() < 0)
			return 0l;
		try {
			gridFsTemplate.delete(query);
		} catch (Exception e) {
			logger.error("删除文件错误", e);
			return 0l;
		}
		return 1l;
	}

	@Override
	public Long deleteById(String fileId) {
		FileInfoBean fileInfoBean = new FileInfoBean(fileId);
		return delete(fileInfoBean);
	}

	@Override
	public InputStream getInputStream(String fileHandler) throws IllegalStateException, IOException {
		return getFile(fileHandler).getGridFsResource().getInputStream();
	}


	@Override
	public Long writeTo(String fileHandler, OutputStream outputStream) throws IOException {
		JbmGridFSDBFile file = getFile(fileHandler);
		if (file != null)
			return file.writeTo(outputStream);
		return null;
	}

	@Override
	public Long writeTo(FileInfoBean fileInfoBean, OutputStream outputStream) throws IOException {
		JbmGridFSDBFile file = getFile(fileInfoBean);
		if (file != null)
			return file.writeTo(outputStream);
		return null;
	}

	@Override
	public FileInfoBean writeToFileInfoBean(String fileId) throws IOException {
		return writeToFileInfoBean(new FileInfoBean(fileId));
	}

	@SuppressWarnings("unchecked")
	@Override
	public FileInfoBean writeToFileInfoBean(FileInfoBean fileInfoBean) throws IOException {
		JbmGridFSDBFile file = getFile(fileInfoBean);
		if (file == null)
			throw new IOException("file not exist");
		FileInfoBean db_fileInfoBean = buildFileInfoBean(file);
		// 将原信息取出来放进去
		if (file.getMetaData() != null)
			db_fileInfoBean.setMetaData(file.getMetaData());
		if (file != null) {
			try {
				writeTo(db_fileInfoBean, true);
			} catch (IOException e) {
				logger.error("写入文件错误", e);
			}
		}
		return db_fileInfoBean;
	}

	@Override
	public byte[] writeTo(FileInfoBean fileInfoBean) throws IOException {
		return writeTo(fileInfoBean, false);
	}

	public byte[] writeTo(FileInfoBean fileInfoBean, boolean write) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytes = null;
		try {
			writeTo(fileInfoBean, baos);
			if (baos.size() > 0) {
				bytes = baos.toByteArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			baos.close();
		}
		if (write)
			fileInfoBean.setFileBytes(bytes);
		return bytes;
	}

	@Override
	public String writeToBase64String(FileInfoBean fileInfoBean) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeTo(fileInfoBean, baos);
		return Base64Utils.encode(baos.toByteArray());
	}

	@Override
	public String writeToString(FileInfoBean fileInfoBean) throws IOException {
		String str = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.flush();
			writeTo(fileInfoBean, baos);
			if (baos.size() > 0)
				str = baos.toString("UTF-8");
		} catch (Exception e) {
			logger.error("写入字符串错误", e);
		} finally {
			baos.close();
		}
		return str;
	}

	@Override
	public JbmGridFSDBFile getFile(FileInfoBean fileInfoBean) {
		Query query = genQuery(fileInfoBean).limit(1);
		return convertGridFSFile2Resource(gridFsTemplate.find(query).first());
	}

	@Override
	public JbmGridFSDBFile getFile(String fileHandler) {
		FileInfoBean fileInfoBean = new FileInfoBean();
		fileInfoBean.setFileName(fileHandler);
		Query query = genQuery(fileInfoBean).limit(1);
		return convertGridFSFile2Resource(gridFsTemplate.find(query).first());
	}

	@Override
	public JbmGridFSDBFile getFileById(String fileId) {
		FileInfoBean fileInfoBean = new FileInfoBean();
		fileInfoBean.setId(fileId);
		Query query = genQuery(fileInfoBean).limit(1);
		return convertGridFSFile2Resource(gridFsTemplate.find(query).first());
	}

	public JbmGridFSDBFile convertGridFSFile2Resource(com.mongodb.client.gridfs.model.GridFSFile gridFsFile) {
		GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFsFile.getObjectId());
		GridFsResource res = new GridFsResource(gridFsFile, gridFSDownloadStream);
		return new JbmGridFSDBFile(gridFSDownloadStream.getGridFSFile(), res);
	}

	protected Query genQuery(FileInfoBean fileInfoBean) {
		Query query = new Query();
		Criteria criteriaDefinition = GridFsCriteria.where("");
		// 如果有ID就直接返回
		if (StringUtils.isNotBlank(fileInfoBean.getId())) {
			criteriaDefinition.and("_id").is(fileInfoBean.getId());
			query.addCriteria(criteriaDefinition);
			return query;
		}
		if (StringUtils.isNotBlank(fileInfoBean.getFileName())) {
			criteriaDefinition.and("filename").is(fileInfoBean.getFileName());
		}
		if (StringUtils.isNotBlank(fileInfoBean.getContentType())) {
			criteriaDefinition.and("contentType").is(fileInfoBean.getContentType());
		}
		if (StringUtils.isNotBlank(fileInfoBean.getContentType())) {
			criteriaDefinition.and("md5").is(fileInfoBean.getMd5());
		}
		query.addCriteria(criteriaDefinition);
		return query;
	}

}

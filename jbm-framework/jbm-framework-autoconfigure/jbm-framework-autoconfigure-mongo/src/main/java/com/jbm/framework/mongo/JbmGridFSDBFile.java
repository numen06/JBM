package com.jbm.framework.mongo;

import cn.hutool.core.io.FileUtil;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.BsonValue;
import org.bson.Document;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public class JbmGridFSDBFile {

    private final BsonValue id;
    private final String filename;
    private final long length;
    private final long chunkSize;
    private final Date uploadDate;
    // Optional values
    private final Document metadata;
//	private final String md5;
    private GridFsResource gridFsResource;

    // Deprecated values
//	private final Document extraElements;

    @SuppressWarnings("deprecation")
    public JbmGridFSDBFile(GridFSFile gridFSFile, GridFsResource gridFsResource) {
        super();
        this.gridFsResource = gridFsResource;
        this.id = gridFSFile.getId();
        this.filename = gridFSFile.getFilename();
        this.length = gridFSFile.getLength();
        this.chunkSize = gridFSFile.getChunkSize();
        this.uploadDate = gridFSFile.getUploadDate();
//		this.md5 = gridFSFile.getMD5();
        this.metadata = gridFSFile.getMetadata();
//		this.extraElements = gridFSFile.getExtraElements();
    }

    public GridFsResource getGridFsResource() {
        return gridFsResource;
    }

    public void setGridFsResource(GridFsResource gridFsResource) {
        this.gridFsResource = gridFsResource;
    }

    public BsonValue getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public long getLength() {
        return length;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

//	public String getMD5() {
//		return md5;
//	}

    public Document getMetaData() {
        return metadata;
    }

//	public Document getExtraElements() {
//		return extraElements;
//	}

    public String getContentType() {
//		if (extraElements != null && extraElements.containsKey("contentType")) {
//			return extraElements.getString("contentType");
//		} else {
//			throw new MongoGridFSException("No contentType result for this GridFS file");
//		}
        return FileUtil.getMimeType(this.filename);
    }

    public Long writeTo(OutputStream outputStream) throws IllegalStateException, IOException {
        return new Long(IOUtils.copy(gridFsResource.getInputStream(), outputStream));
    }

}

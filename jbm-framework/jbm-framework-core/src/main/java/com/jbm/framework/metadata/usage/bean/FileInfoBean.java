package com.jbm.framework.metadata.usage.bean;

import com.jbm.util.PathUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件流传输类
 *
 * @author wesley
 */
public class FileInfoBean implements PrimaryKey<String> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID
     */
    private String id;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 远程文件路径
     */
    private String filePath;

    /**
     * 本地文件地址
     */
    private String localFilePath;
    /**
     * 文件类型
     */
    private String contentType;
    /**
     * 文件大小
     */
    private Long length;
    /**
     * 分段大小
     */
    private Long chunkSize;
    /**
     * 上传时间
     */
    private Date uploadDate;
    /**
     * 字节流校验
     */
    private String md5;
    /**
     * 原名称
     */
    private String originalName;
    /**
     * 文件字节流
     */
    private byte[] fileBytes;
    /**
     * 失效时间,-1表示不失效,单位是秒
     */
    private Long expiredTime;
    /**
     * 作废时间,具体是什么时间删除的
     */
    private Date invalidTime;

    /**
     * 原来的ID用来追溯版本号
     */
    private String originaId;

    /**
     * 组别
     */
    private String group;

    /**
     * 版本号
     */
    private String versionNumber;

    /**
     * 应用
     */
    private String appKey;

    /**
     * 扩展名
     */
    private String extension;

    /**
     * 扩展信息
     */
    private Map<String, Object> metaData = new HashMap<String, Object>();

    public FileInfoBean() {
        super();
    }

    public FileInfoBean(String id) {
        super();
        this.id = id;
    }

    public FileInfoBean(final File file) throws IOException {
        super();
        if (file.exists()) {
            this.fileName = file.getName();
            this.contentType = PathUtils.getExtension(file.getPath());
            this.originalName = file.getName();
            this.fileBytes = FileUtils.readFileToByteArray(file);
            this.length = file.length();
        }
    }

    public FileInfoBean(String fileName, String contentType, String originalName, byte[] fileBytes) {
        super();
        this.fileName = fileName;
        this.contentType = contentType;
        this.originalName = originalName;
        this.fileBytes = fileBytes;
    }

    public FileInfoBean(String fileName, String contentType, Long length, Long chunkSize, Date uploadDate, String md5) {
        super();
        this.fileName = fileName;
        this.contentType = contentType;
        this.length = length;
        this.chunkSize = chunkSize;
        this.uploadDate = uploadDate;
        this.md5 = md5;
    }

    public FileInfoBean(String fileName, String contentType, Long length, Long chunkSize, Date uploadDate, String md5, String originalName) {
        super();
        this.fileName = fileName;
        this.contentType = contentType;
        this.length = length;
        this.chunkSize = chunkSize;
        this.uploadDate = uploadDate;
        this.md5 = md5;
        this.originalName = originalName;
    }

    public FileInfoBean(String id, String fileName, String contentType, Long length, Long chunkSize, Date uploadDate, String md5, String originalName) {
        super();
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.length = length;
        this.chunkSize = chunkSize;
        this.uploadDate = uploadDate;
        this.md5 = md5;
        this.originalName = originalName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public Date getInvalidTime() {
        return invalidTime;
    }

    public void setInvalidTime(Date invalidTime) {
        this.invalidTime = invalidTime;
    }

    public Map<String, Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }

    public String getOriginaId() {
        return originaId;
    }

    public void setOriginaId(String originaId) {
        this.originaId = originaId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

}

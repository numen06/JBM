package com.jbm.framework.metadata.usage.bean;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 图片信息封装类
 * 
 * @author wesley
 *
 */
public class ImageInfoBean extends FileInfoBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 宽度
	 */
	private Integer width;
	/**
	 * 高度
	 */
	private Integer height;
	/**
	 * 
	 */
	private String originalImageId;
	/**
	 * 拍摄时间
	 */
	private Date shootTime;
	/**
	 * 海拔
	 */
	private String altitude;
	/**
	 * 纬度
	 */
	private String latitude;
	/**
	 * 经度
	 */
	private String longitude;

	/**
	 * 图片分组,用于缩放图
	 */
	private String imageGroup;

	public ImageInfoBean() {
		super();
	}

	public ImageInfoBean(final File file) throws IOException {
		super(file);
	}

	public ImageInfoBean(FileInfoBean bean) {
		super(bean.getId(), bean.getFileName(), bean.getContentType(), bean.getLength(), bean.getChunkSize(), bean.getUploadDate(), bean.getMd5(), bean.getOriginalName());
		super.setFileBytes(bean.getFileBytes());
		super.setMetaData(bean.getMetaData());
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public String getOriginalImageId() {
		return originalImageId;
	}

	public void setOriginalImageId(String originalImageId) {
		this.originalImageId = originalImageId;
	}

	public String getAltitude() {
		return altitude;
	}

	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public Date getShootTime() {
		return shootTime;
	}

	public void setShootTime(Date shootTime) {
		this.shootTime = shootTime;
	}

	public String getImageGroup() {
		return imageGroup;
	}

	public void setImageGroup(String imageGroup) {
		this.imageGroup = imageGroup;
	}

}

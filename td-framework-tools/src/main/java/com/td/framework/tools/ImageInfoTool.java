package com.td.framework.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.td.framework.metadata.usage.bean.FileInfoBean;
import com.td.framework.metadata.usage.bean.ImageInfoBean;
import com.td.util.TimeUtil;

/**
 * 图片信息获取
 * 
 * @author wesley
 *
 */
public class ImageInfoTool {
	/**
	 * 图片信息获取metadata元数据信息
	 * 
	 * @param fileName
	 *            需要解析的文件
	 * @return
	 */
	public static ImageInfoBean parseImgInfo(String fileName) {
		File file = new File(fileName);
		ImageInfoBean imgInfoBean = null;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			imgInfoBean = printImageTags(new ImageInfoBean(new FileInfoBean(file)), metadata);
		} catch (ImageProcessingException e) {
			System.err.println("error 1a: " + e);
		} catch (IOException e) {
			System.err.println("error 1b: " + e);
		}
		return imgInfoBean;
	}

	public static ImageInfoBean parseImgInfo(ImageInfoBean imageInfoBean) {
		ImageInfoBean imgInfoBean = null;
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(imageInfoBean.getFileBytes());
			Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
			imgInfoBean = printImageTags(imageInfoBean, metadata);
		} catch (ImageProcessingException e) {
			System.err.println("error 1a: " + e);
		} catch (IOException e) {
			System.err.println("error 1b: " + e);
		}
		return imgInfoBean;
	}

	/**
	 * 读取metadata里面的信息
	 * 
	 * @param sourceFile
	 *            源文件
	 * @param metadata
	 *            metadata元数据信息
	 * @return
	 */
	private static ImageInfoBean printImageTags(ImageInfoBean imageInfoBean, Metadata metadata) {
		for (Directory directory : metadata.getDirectories()) {
			for (Tag tag : directory.getTags()) {
				String tagName = tag.getTagName();
				String desc = tag.getDescription();
				imageInfoBean.getMetaData().put(MessageFormat.format("[{0}] {1}", directory.getName(), tagName), desc);
				if (tagName.equals("Image Height")) {
					// 图片高度
					// System.out.println(desc);
					imageInfoBean.setHeight(Integer.parseInt(StringUtils.substringBefore(desc, " ")));
				} else if (tagName.equals("Image Width")) {
					// System.out.println(desc);
					// 图片宽度
					imageInfoBean.setWidth(Integer.parseInt(StringUtils.substringBefore(desc, " ")));
				} else if (tagName.equals("Date/Time Original")) {
					// 拍摄时间
					imageInfoBean.setShootTime(TimeUtil.softParseDate(desc));
				} else if (tagName.equals("GPS Altitude")) {
					// 海拔
					imageInfoBean.setAltitude(desc);
				} else if (tagName.equals("GPS Latitude")) {
					// 纬度
					imageInfoBean.setLatitude(pointToLatlong(desc));
				} else if (tagName.equals("GPS Longitude")) {
					// 经度
					imageInfoBean.setLongitude(pointToLatlong(desc));
				}
			}
			for (String error : directory.getErrors()) {
				System.err.println("ERROR: " + error);
			}
		}
		return imageInfoBean;
	}

	/**
	 * 经纬度转换 度分秒转换
	 * 
	 * @param point
	 *            坐标点
	 * @return
	 */
	private static String pointToLatlong(String point) {
		Double du = Double.parseDouble(point.substring(0, point.indexOf("°")).trim());
		Double fen = Double.parseDouble(point.substring(point.indexOf("°") + 1, point.indexOf("'")).trim());
		Double miao = Double.parseDouble(point.substring(point.indexOf("'") + 1, point.indexOf("\"")).trim());
		Double duStr = du + fen / 60 + miao / 60 / 60;
		return duStr.toString();
	}
}

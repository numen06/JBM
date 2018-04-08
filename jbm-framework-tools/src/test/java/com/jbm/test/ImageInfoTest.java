package com.jbm.test;

import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.usage.bean.ImageInfoBean;
import com.jbm.framework.tools.ImageInfoTool;

public class ImageInfoTest {

	public static void main(String[] args) {
		try {
			ImageInfoBean imgInfoBean = ImageInfoTool.parseImgInfo("C:\\Users\\wesley\\Desktop\\PIC_20130612_192413_910.jpg");
			imgInfoBean.setFileBytes(null);
			System.out.println(JSON.toJSONString(imgInfoBean));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

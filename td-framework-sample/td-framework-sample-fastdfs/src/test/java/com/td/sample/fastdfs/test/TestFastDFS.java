package com.td.sample.fastdfs.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.fastdfs.FastdfsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.td.sample.fastdfs.FastdfsAppStart;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import td.framework.boot.autoconfigure.fastdfs.FastdfsAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FastdfsAppStart.class)
@Import(FastdfsAutoConfiguration.class)
public class TestFastDFS {

	@Autowired
	private FastdfsTemplate fastdfsTemplate;

	@Test
	public void testUpload() throws Exception {
		String fileName = "D:\\360极速浏览器下载\\智慧园区系统展示V.20180109.1.mp4";
		String result = fastdfsTemplate.uploadFile(fileName, FileNameUtil.getExtension(fileName));
		String fileId = result;
		System.out.println(fileId);
		System.out.println(fastdfsTemplate.getFileInfo(fileId));
		System.out.println(fastdfsTemplate.getMetadata(fileId));
		System.out.println(fastdfsTemplate.queryFileInfo(fileId));
		// System.out.println(fastdfsTemplate.deleteFile(fileId));
		FileUtil.writeBytes("test.mp4", fastdfsTemplate.downloadFile(fileId));
		System.err.println(result);
	}

	// @Test
	// public void load() throws IOException {
	// System.out.println(PropertiesLoaderUtils.loadAllProperties("fastdfs_client.conf"));
	// }
}

package com.td.test.devops;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.h2.mvstore.cache.CacheLongKeyLIRS.Config;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.jimfs.Jimfs;
import com.td.framework.metadata.usage.bean.FileInfoBean;
import com.td.util.FileUtils;
import com.td.util.StringUtils;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import junit.framework.TestCase;

public class FileTest extends TestCase {

	public void testReq() {
		HttpResponse httpResponse = HttpRequest.post("http://192.168.14.214:19999/source/channelRelease").form("ope", "a").form("json", "b").send();
		String fileResponse = JSON.parseObject(httpResponse.body(), HashMap.class).toString();
		System.out.println(fileResponse);
	}

	public void testUpload2() throws IOException {
		File file = new File("C:\\Users\\chen\\Desktop\\td-evop-program.jar");
		// File file = FileUtils.getFile("‪G:\\dev\\td-dpen-center-web.jar");
		// FileUtils.readFileToByteArray(file);
		HttpResponse httpResponse = HttpRequest.post("http://192.168.1.199:20000/doc/uploadDocument").form("file", file).form("filename", file.getName()).send();
		String fileResponse = JSON.parseObject(httpResponse.body(), HashMap.class).get("result").toString();
		List<FileInfoBean> beans = JSON.parseArray(fileResponse, FileInfoBean.class);
		System.err.println(beans);
	}

	public void testUpload() {
		HttpResponse httpResponse = HttpRequest.post("http://127.0.0.1:8080/source/deploy")
			.form("file", new File("D:/workspace/td-framework/td-framework-sample/td-framework-sample-restapi/target/td-framework-sample-restapi.jar")).send();
		String id = (String) JSON.parseObject(httpResponse.body(), HashMap.class).get("result");
		System.err.println(id);
		HttpResponse httpResponse2 = HttpRequest.post("http://127.0.0.1:8080/source/release").form("sourceInfoName", id).send();
		System.err.println(httpResponse2.body());
	}

	public void testFindFiles() {
		Collection<File> files = Lists.newArrayList(FileUtils.filesMatchingStemRegex(new File("."), ".*.pid"));
		System.out.println(StringUtils.join(files, "\n"));
	}

	public void testJimFs() throws IOException {
		System.out.println(JSON.toJSONString(Jimfs.newFileSystem("/").getRootDirectories()));
	}
}

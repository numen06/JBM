package com.jbm.test.http;

import java.io.File;
import java.io.IOException;

import org.tio.core.Node;

import jbm.framework.boot.autoconfigure.tio.AioHttpClientTemplate;
import jbm.framework.boot.autoconfigure.tio.handler.HttpResponseHandler;

public class HttpClientTest {

	public static void main(String[] args) throws IOException {
		AioHttpClientTemplate aioHttpClientTemplate = new AioHttpClientTemplate(new Node("127.0.0.1", 9090), new HttpResponseHandler() {

			@Override
			public jodd.http.HttpRequest handler(jodd.http.HttpResponse packet) throws Exception {
				System.out.println(packet.bodyText());
				return null;
			}
		});
		jodd.http.HttpRequest httpRequest = jodd.http.HttpRequest.post("http://127.0.0.1:9090/test/upload");
		File file = new File("D:\\360极速浏览器下载\\win10dpixf_veryhuo.com.rar");
			httpRequest.form("uploadFile", file).form("filename", file.getName());
//		httpRequest.body("12312313");
		httpRequest.connectionKeepAlive(true);
		aioHttpClientTemplate.send(httpRequest);
	}
}

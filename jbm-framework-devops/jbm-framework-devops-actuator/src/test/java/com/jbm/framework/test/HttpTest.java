package com.jbm.framework.test;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import junit.framework.TestCase;

public class HttpTest extends TestCase {

	public void testGetFile() {
		HttpRequest httpRequest = HttpRequest.get("http://doc.tdenergys.com/doc/588211169c46b5a3b8d02826.jar");
		HttpResponse response = httpRequest.send();
		String filanme = response.header("content-disposition");
		System.err.println(filanme);
		System.out.println(response.bodyBytes());
	}

}

package com.td.test.ftp;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.ftp.FtpTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import td.framework.boot.autoconfigure.ftp.FtpAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FtpAutoConfiguration.class)
public class FtpTest {

	@Autowired
	private FtpTemplate ftpTemplate;

	@Test
	public void testFtp() throws IOException {
		// System.out.println(JSON.toJSONString(ftpTemplate.getFtpClient().listFiles()));
		FTPFile[] fs = ftpTemplate.getFtpClient().listFiles();
		String localpath = "ftp/";
		for (int i = 0; i < fs.length; i++) {
			ftpTemplate.syncDir("", localpath);
		}
	}
}

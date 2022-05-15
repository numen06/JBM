package com.jbm.test.ftp;

import jbm.framework.boot.autoconfigure.ftp.FtpAutoConfiguration;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.ftp.FtpTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
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

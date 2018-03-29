package org.springframework.data.ftp;

import java.io.IOException;
import java.net.SocketException;

import td.framework.boot.autoconfigure.ftp.FtpProperties;

public class FtpTemplateFactory {

	private FtpTemplateFactory() {

	}

	public static FtpTemplate createFtpTemplate(String hostname, Integer port, String username, String password) throws SocketException, IOException {
		FtpProperties ftpProperties = new FtpProperties();
		ftpProperties.setHostname(hostname);
		ftpProperties.setPort(port);
		ftpProperties.setUsername(username);
		ftpProperties.setPassword(password);
		FtpTemplate ftpTemplate = new FtpTemplate(ftpProperties);
		return ftpTemplate;
	}

}

package com.jbm.framework.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import com.jbm.util.StringUtils;

/**
 *
 */
public class FTPClientTools {

	public static final Log log = LogFactory.getLog(FTPClientTools.class);

	// FTP server configuration--IP key,value is type of String
	public static final String SERVER_IP = "SERVER_IP";

	// FTP server configuration--Port key,value is type of Integer
	public static final String SERVER_PORT = "SERVER_PORT";

	// FTP server configuration--ANONYMOUS Log in key, value is type of Boolean
	public static final String IS_ANONYMOUS = "IS_ANONYMOUS";

	// user name of anonymous log in
	public static final String ANONYMOUS_USER_NAME = "anonymous";

	// password of anonymous log in
	public static final String ANONYMOUS_PASSWORD = "";

	// FTP server configuration--log in user name, value is type of String
	public static final String USER_NAME = "USER_NAME";

	// FTP server configuration--log in password, value is type of String
	public static final String PASSWORD = "PASSWORD";

	// FTP server configuration--PASV key, value is type of Boolean
	public static final String IS_PASV = "IS_PASV";

	// FTP server configuration--working directory key, value is type of String
	// While logging in, the current directory is the user's home directory,
	// the workingDirectory must be set based on it.
	// Besides, the workingDirectory must exist, it can not be created
	// automatically.
	// If not exist, file will be uploaded in the user's home directory.
	// If not assigned, "/" is used.
	public static final String WORKING_DIRECTORY = "WORKING_DIRECTORY";

	/**
	 * Upload a file to FTP server.
	 * 
	 * @param serverCfg
	 *            : FTP server configuration
	 * @param filePathToUpload
	 *            : path of the file to upload
	 * @param fileStoredName
	 *            : the name to give the remote stored file, null, "" and other
	 *            blank word will be replaced by the file name to upload
	 * @throws IOException
	 * @throws SocketException
	 */
	public static final void upload(Map<String, Object> serverCfg, String filePathToUpload, String fileStoredName) throws SocketException, IOException {
		upload(serverCfg, new File(filePathToUpload), fileStoredName);
	}

	/**
	 * Upload a file to FTP server.
	 * 
	 * @param serverCfg
	 *            : FTP server configuration
	 * @param fileToUpload
	 *            : file to upload
	 * @param fileStoredName
	 *            : the name to give the remote stored file, null, "" and other
	 *            blank word will be replaced by the file name to upload
	 * @throws IOException
	 * @throws SocketException
	 */
	public static final void upload(Map<String, Object> serverCfg, File fileToUpload, String fileStoredName) throws SocketException, IOException {
		if (!fileToUpload.exists()) {
			throw new IllegalArgumentException("File to upload does not exists：" + fileToUpload.getAbsolutePath());
		}
		if (!fileToUpload.isFile()) {
			throw new IllegalArgumentException("File to upload is not a file:" + fileToUpload.getAbsolutePath());
		}
		if (StringUtils.isBlank((String) serverCfg.get(SERVER_IP))) {
			throw new IllegalArgumentException("SERVER_IP must be contained in the FTP server configuration.");
		}
		transferFile(true, serverCfg, fileToUpload, fileStoredName, null, null);
	}

	/**
	 * Download a file from FTP server
	 * 
	 * @param serverCfg
	 *            : FTP server configuration
	 * @param fileNameToDownload
	 *            : file name to be downloaded
	 * @param fileStoredPath
	 *            : stored path of the downloaded file in local
	 * @throws SocketException
	 * @throws IOException
	 */
	public static final void download(Map<String, Object> serverCfg, String fileNameToDownload, String fileStoredPath) throws SocketException, IOException {
		if (StringUtils.isBlank(fileNameToDownload)) {
			throw new IllegalArgumentException("File name to be downloaded can not be blank.");
		}
		if (StringUtils.isBlank(fileStoredPath)) {
			throw new IllegalArgumentException("Stored path of the downloaded file in local can not be blank.");
		}
		if (StringUtils.isBlank((String) serverCfg.get(SERVER_IP))) {
			throw new IllegalArgumentException("SERVER_IP must be contained in the FTP server configuration.");
		}
		transferFile(false, serverCfg, null, null, fileNameToDownload, fileStoredPath);
	}

	private static final void transferFile(boolean isUpload, Map<String, Object> serverCfg, File fileToUpload, String serverFileStoredName, String fileNameToDownload,
		String localFileStoredPath) throws SocketException, IOException {
		String host = (String) serverCfg.get(SERVER_IP);
		Integer port = (Integer) serverCfg.get(SERVER_PORT);
		Boolean isAnonymous = (Boolean) serverCfg.get(IS_ANONYMOUS);
		String username = (String) serverCfg.get(USER_NAME);
		String password = (String) serverCfg.get(PASSWORD);
		Boolean isPASV = (Boolean) serverCfg.get(IS_PASV);
		String workingDirectory = (String) serverCfg.get(WORKING_DIRECTORY);
		FTPClient ftpClient = new FTPClient();
		InputStream fileIn = null;
		OutputStream fileOut = null;
		try {
			if (port == null) {
				log.debug("Connect to FTP server on " + host + ":" + FTP.DEFAULT_PORT);
				ftpClient.connect(host);
			} else {
				log.debug("Connect to FTP server on " + host + ":" + port);
				ftpClient.connect(host, port);
			}
			int reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				log.error("FTP server refuses connection");
				return;
			}

			if (isAnonymous != null && isAnonymous) {
				username = ANONYMOUS_USER_NAME;
				password = ANONYMOUS_PASSWORD;
			}
			log.debug("Log in FTP server with username = " + username + ", password = " + password);
			if (!ftpClient.login(username, password)) {
				log.error("Fail to log in FTP server with username = " + username + ", password = " + password);
				ftpClient.logout();
				return;
			}

			// Here we will use the BINARY mode as the transfer file type,
			// ASCII mode is not supportted.
			log.debug("Set type of the file, which is to upload, to BINARY.");
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			if (isPASV != null && isPASV) {
				log.debug("Use the PASV mode to transfer file.");
				ftpClient.enterLocalPassiveMode();
			} else {
				log.debug("Use the ACTIVE mode to transfer file.");
				ftpClient.enterLocalActiveMode();
			}

			if (StringUtils.isBlank(workingDirectory)) {
				workingDirectory = "/";
			}
			log.debug("Change current working directory to " + workingDirectory);
			ftpClient.changeWorkingDirectory(workingDirectory);

			if (isUpload) { // upload
				if (StringUtils.isBlank(serverFileStoredName)) {
					serverFileStoredName = fileToUpload.getName();
				}
				fileIn = new FileInputStream(fileToUpload);
				log.debug("Upload file : " + fileToUpload.getAbsolutePath() + " to FTP server with name : " + serverFileStoredName);
				if (!ftpClient.storeFile(serverFileStoredName, fileIn)) {
					log.error("Fail to upload file, " + ftpClient.getReplyString());
				} else {
					log.debug("Success to upload file.");
				}
			} else { // download
				// make sure the file directory exists
				File fileStored = new File(localFileStoredPath);
				if (!fileStored.getParentFile().exists()) {
					fileStored.getParentFile().mkdirs();
				}
				fileOut = new FileOutputStream(fileStored);
				log.debug("Download file : " + fileNameToDownload + " from FTP server to local : " + localFileStoredPath);
				if (!ftpClient.retrieveFile(fileNameToDownload, fileOut)) {
					log.error("Fail to download file, " + ftpClient.getReplyString());
				} else {
					log.debug("Success to download file.");
				}
			}

			ftpClient.noop();

			ftpClient.logout();

		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException f) {
				}
			}
			if (fileIn != null) {
				try {
					fileIn.close();
				} catch (IOException e) {
				}
			}
			if (fileOut != null) {
				try {
					fileOut.close();
				} catch (IOException e) {
				}
			}
		}
	}

}

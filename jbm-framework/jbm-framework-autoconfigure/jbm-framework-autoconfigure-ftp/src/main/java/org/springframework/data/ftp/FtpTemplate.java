package org.springframework.data.ftp;

import com.jbm.util.FileNameUtils;
import jbm.framework.boot.autoconfigure.ftp.FtpProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;

/**
 * @author wesley.zhang
 */
public class FtpTemplate {

    private static Logger logger = LoggerFactory.getLogger(FtpTemplate.class);

    private FTPClient ftpClient = new FTPClient();

    private FtpProperties ftpProperties;

    public FtpTemplate() {
        super();
    }

    public FtpTemplate(FtpProperties ftpProperties) throws SocketException, IOException {
        this.ftpProperties = ftpProperties;
        logger.info("ftp connect ,hostname:{} pot:{}", ftpProperties.getHostname(), ftpProperties.getPort());
        reconnect(ftpProperties);
    }

    public FTPClient getFtpClient() {
        try {
            return reconnect(ftpClient);
        } catch (IOException e) {
            return null;
        }
    }

    public FTPClient reconnect(FTPClient ftpClient) throws IOException {
        if (ftpClient.isConnected()) {
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
            }
        }
        ftpClient.connect(ftpProperties.getHostname(), ftpProperties.getPort());
        ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPassword());
        return ftpClient;
    }

    public void syncDir(String root, String localpath) throws IOException {
        FTPFile[] fs = this.ftpClient.listFiles(root);
        for (int i = 0; i < fs.length; i++) {
            FTPFile fiFtpFile = fs[i];
            String ftpDir = root;
            String localDir = localpath;
            if (fiFtpFile.isDirectory()) {
                ftpDir = FileNameUtils.concat(ftpDir, fiFtpFile.getName(), true);
                localDir = FileNameUtils.concat(localDir, fiFtpFile.getName());
                FileUtils.forceMkdir(new File(localDir));
                this.syncDir(ftpDir, localDir);
            } else {
                String ftpFile = FileNameUtils.concat(ftpDir, fiFtpFile.getName(), true);
                String localFile = FileNameUtils.concat(localDir, fiFtpFile.getName(), true);
                FileOutputStream fileOutputStream = new FileOutputStream(localFile);
                this.ftpClient.retrieveFile(ftpFile, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            }
        }
    }

}

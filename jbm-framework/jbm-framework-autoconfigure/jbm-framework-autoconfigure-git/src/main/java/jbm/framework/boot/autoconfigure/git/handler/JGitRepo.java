package jbm.framework.boot.autoconfigure.git.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;

import java.io.File;
import java.io.IOException;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-18 01:00
 **/
@Slf4j
public class JGitRepo {

    /**
     * 本地仓库地址
     */
    private File localRepo;
    private File localRepoGit;
    /**
     * "git@github.com:numen06/JBM.git"
     */
    private String url;
    private Git git;

    public JGitRepo(String url, String localPath) {
        this.url = url;
        this.localRepo = new File(localPath);
    }

    public JGitRepo(String url, File localRepo) {
        this.url = url;
        this.localRepo = localRepo;
    }

    public JGitRepo(String url) {
        this.url = url;
        String name = StrUtil.subBetween(url, "/", ".git");
        this.localRepo = new File(name);
    }


    public void init() throws IOException, GitAPIException {
        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
        });
        if (git == null) {
            if (FileUtil.exist(localRepo)) {
                this.gitOpen();
            } else {
                this.gitChone();
            }
        }

        git.fetch().call();
    }


    public void gitOpen() throws IOException {
        this.git = Git.open(localRepo);
    }


    public void gitChone() throws IOException, GitAPIException {
        if (!FileUtil.del(localRepo)) {
            throw new IOException("Could not delete temporary file " + localRepo);
        }
        // then clone
        log.info("Cloning from {} to {}", url, localRepo);
        try (Git result = Git.cloneRepository()
                .setURI(url)
                .setDirectory(localRepo)
                .setProgressMonitor(new SimpleProgressMonitor())
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            log.info("Having repository: " + result.getRepository().getDirectory());
            this.git = result;
        }


    }


}

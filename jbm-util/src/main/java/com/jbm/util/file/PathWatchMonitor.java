package com.jbm.util.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;

@Slf4j
public class PathWatchMonitor {

    private Path sourceFolderPath;
    private Path targetFolderPath;

    private WatchMonitor watchMonitor;


    private void copyModifiedFile() {
        try {
//            Path targetFilePath = Paths.get(targetFolderPath.toString(), sourceFile.getFileName().toString());
            FileUtil.copy(sourceFolderPath.toFile(), targetFolderPath.getParent().toFile(), true);
            System.out.println("Modified file copied: " + sourceFolderPath + " --> " + targetFolderPath);
        } catch (Exception e) {
            System.err.println("Failed to copy modified file: " + e.getMessage());
        }
    }

    public void watch(String classpath) {
        // 获取classpath下源文件夹路径
        ClassPathResource targetFolderResource = new ClassPathResource("");
        this.targetFolderPath = Paths.get(targetFolderResource.getAbsolutePath()).resolve(classpath);
        // 获取项目源码下的目标文件夹路径（相对路径）
        String sourceFolderPathRelativeToProjectRoot = StrUtil.contains(this.targetFolderPath.toAbsolutePath().toString(), "test-classes") ? "src/test/resources" : "src/main/resources";
        this.sourceFolderPath = targetFolderPath.getParent().getParent().getParent().resolve(sourceFolderPathRelativeToProjectRoot).resolve(classpath);

        log.info("Watching source folder: " + sourceFolderPath.toFile().getAbsolutePath());
        log.info("Copying modified files to target folder: " + targetFolderPath.toFile().getAbsolutePath());

        // 确保目标文件夹存在
        FileUtil.mkParentDirs(targetFolderPath);

        this.watchMonitor = WatchMonitor.createAll(sourceFolderPath, new SimpleWatcher() {
            @Override
            public void onModify(WatchEvent<?> event, Path sourceFile) {
                copyModifiedFile();
            }
        });
        this.watchMonitor.setMaxDepth(2);
    }

    public void start() {
        watchMonitor.start();
    }

    public void stop() {
        watchMonitor.close();
    }

}

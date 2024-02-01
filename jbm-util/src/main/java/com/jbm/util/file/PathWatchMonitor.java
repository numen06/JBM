package com.jbm.util.file;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.util.Date;

@Slf4j
public class PathWatchMonitor {

    private Path sourceFolderPath;
    private Path targetFolderPath;
    private WatchMonitor watchMonitor;

    private int delay;
    private int maxDelay;

    public PathWatchMonitor() {
        this(1000, 10);
    }

    public PathWatchMonitor(int delay, int maxDelay) {
        this.delay = delay;
        this.maxDelay = maxDelay;
    }

    private synchronized void copyFileOrDirectory(Path source) {
        try {
            Files.copy(source, targetFolderPath.resolve(sourceFolderPath.relativize(source)), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            log.info("File " + source + " --> " + targetFolderPath.resolve(sourceFolderPath.relativize(source)) + " is created.");
        } catch (Exception e) {
            log.error("Failed to create directory: " + e.getMessage());
        }
    }

    private synchronized void deleteCorrespondingFile(Path source) {
        try {
            Files.deleteIfExists(targetFolderPath.resolve(sourceFolderPath.relativize(source)));
            log.info("File " + source + " is deleted.");
        } catch (Exception e) {
            log.error("Failed to delete file: " + e.getMessage());
        }
    }


    private synchronized void copyUpdatedFile(Path source) {
        try {
            String relativePath = sourceFolderPath.relativize(source).toString();
            // 检查路径是否为空
            if (StrUtil.isBlank(relativePath)) {
                return;
            }
            // 检查文件名是否以~结尾
            if (relativePath.endsWith("~")) {
                return;
            }
            Path target = targetFolderPath.resolve(relativePath);
            if (FileUtil.isDirectory(target)) {
                // 检查文件夹是否存在
                if (!FileUtil.exist(source.toFile())) {
                    this.deleteCorrespondingFile(source);
                }
                return;
            }
            // 检查目标文件是否存在
            if (!Files.exists(target)) {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                log.info("File " + source + " --> " + target + " is created.");
                return;
            }
            Date targetModifiedTime = FileUtil.lastModifiedTime(target.toFile());
            Date sourceModifiedTime = FileUtil.lastModifiedTime(source.toFile());
            //说明删除了
            if (sourceModifiedTime == null) {
                deleteCorrespondingFile(source);
                return;
            }
            boolean contentChanged = !DateUtil.isSameTime(targetModifiedTime, sourceModifiedTime);

            if (!contentChanged) {
//            log.info("File " + source + " is unchanged. Skipping.");
                return;
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            log.info("File " + source + " --> " + target + " is updated.");
        } catch (Exception e) {
            log.error("Failed to update file: {} ", source, e);
        }
    }

    public void watch() {
        this.watch("");
    }

    public void watch(String classpath) {
        // 获取classpath下源文件夹路径
        ClassPathResource targetFolderResource = new ClassPathResource("");
        this.targetFolderPath = Paths.get(targetFolderResource.getFile().getAbsolutePath()).resolve(classpath);

        // 获取项目源码下的目标文件夹路径（相对路径）
        String sourceFolderPathRelativeToProjectRoot = StrUtil.contains(this.targetFolderPath.toAbsolutePath().toString(), "test-classes") ? "src/test/resources" : "src/main/resources";
        if (StrUtil.isBlank(classpath)) {
            this.sourceFolderPath = targetFolderPath.getParent().getParent().resolve(sourceFolderPathRelativeToProjectRoot).resolve(classpath);
        } else {
            this.sourceFolderPath = targetFolderPath.getParent().getParent().getParent().resolve(sourceFolderPathRelativeToProjectRoot).resolve(classpath);
        }
        log.info("Watching source folder: " + sourceFolderPath.toFile().getAbsolutePath());
        log.info("Copying  target folder: " + targetFolderPath.toFile().getAbsolutePath());

        // 确保目标文件夹存在
        PathUtil.mkdir(targetFolderPath);
        //监听文件修改
        SimpleWatcher watcher = new SimpleWatcher() {

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path changedPath = currentPath.resolve(pathEvent.context());
                log.info("File " + changedPath + " is changed.");
//                String relativePath = sourceFolderPath.relativize(changedPath).toString();
                switch (event.kind().name()) {
                    case "ENTRY_CREATE":
                        copyFileOrDirectory(changedPath);
                        break;
                    case "ENTRY_MODIFY":
                        copyUpdatedFile(changedPath);
                        break;
                    case "ENTRY_DELETE":
                        deleteCorrespondingFile(changedPath);
                        break;
                }
            }
        };
        this.watchMonitor = WatchMonitor.createAll(sourceFolderPath, new DelayWatcher(watcher, this.delay));
        this.watchMonitor.setMaxDepth(this.maxDelay);

        // 启动监听服务
        this.watchMonitor.start();
        log.info("Watching started.");
    }


    public void stop() {
        watchMonitor.close();
    }

}

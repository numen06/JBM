package jbm.framework.boot.autoconfigure.base.listener;

import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

public class Listner implements Runnable {

    protected String rootPath;
    private WatchService service;
    private String fileName;
    private FileWatchCallback callback;

    public Listner(WatchService service, String rootPath, String fileName, FileWatchCallback callback) {
        super();
        this.service = service;
        this.rootPath = rootPath;
        this.fileName = fileName;
        this.callback = callback;
    }

    public void run() {
        try {
            while (true) {
                WatchKey watchKey = service.take();
                List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                for (WatchEvent<?> event : watchEvents) {
                    // TODO 根据事件类型采取不同的操作。。。。。。。
                    // System.out.println("[" + rootPath + "/" + event.context()
                    // + "]文件发生了[" + event.kind() + "]事件");
                    if (fileName.equals(event.context().toString())) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            callback.create(event);
                        }
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            callback.delete(event);
                        }
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            callback.modify(event);
                        }
                    }
                }
                watchKey.reset();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                service.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

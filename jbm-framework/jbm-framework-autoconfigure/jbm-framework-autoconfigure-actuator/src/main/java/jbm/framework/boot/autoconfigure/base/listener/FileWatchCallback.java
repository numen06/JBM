package jbm.framework.boot.autoconfigure.base.listener;

import java.io.File;
import java.nio.file.WatchEvent;

public interface FileWatchCallback {

	void modify(WatchEvent<?> event);

	void create(WatchEvent<?> event);

	void delete(WatchEvent<?> event);

	File getWatchFile();
}

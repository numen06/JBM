package td.framework.boot.autoconfigure.base.listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourceListener {
	private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);
	private WatchService ws;
	private final FileWatchCallback callback;
	private final File listenerFile;

	public ResourceListener(FileWatchCallback callback) {
		super();
		this.callback = callback;
		this.listenerFile = callback.getWatchFile();
	}

	private void start() throws IOException {
		if (ws == null) {
			this.ws = FileSystems.getDefault().newWatchService();
		}
		String path = null;
		String fileName = null;
		if (listenerFile.isDirectory()) {
			path = listenerFile.toURI().getPath();
		} else {
			path = new File(listenerFile.toURI()).getParentFile().getPath();
			fileName = listenerFile.getName();
		}
		Path p = Paths.get(path);
		p.register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
		fixedThreadPool.execute(new Listner(ws, path, fileName, callback));
	}

	public static void addListener(FileWatchCallback callback) throws IOException {
		try {
			ResourceListener resourceListener = new ResourceListener(callback);
			resourceListener.start();
		} catch (IOException e) {
			System.err.println(e);
			throw e;
		}
	}

}

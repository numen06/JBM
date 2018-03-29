package td.framework.boot.autoconfigure.base.listener;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.WatchEvent;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

public class FileWatchCallbackBean implements FileWatchCallback {

	private ApplicationContext applicationContext;

	public FileWatchCallbackBean(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void modify(WatchEvent<?> event) {
		// logger.info("[{}]文件发生了[{}]事件", event.context(), event.kind());
	}

	@Override
	public void create(WatchEvent<?> event) {
		// logger.info("[{}]文件发生了[{}]事件", event.context(), event.kind());
	}

	@Override
	public void delete(WatchEvent<?> event) {
		// logger.info("[{}]文件发生了[{}]事件", event.context(), event.kind());
		SpringApplication.exit(applicationContext, () -> 0);
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		try {
			String os = System.getProperty("os.name");
			if (os.toLowerCase().startsWith("win")) {
				Runtime.getRuntime().exec("tskill " + pid);
			} else {
				Runtime.getRuntime().exec("kill -9 " + pid);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public File getWatchFile() {
		return new File(System.getProperty("application.name"));
	}

}

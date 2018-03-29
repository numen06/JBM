package td.framework.boot.autoconfigure.base.listener;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.core.env.ConfigurableEnvironment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import jodd.io.FileUtil;
import jodd.util.StringUtil;
import td.framework.boot.autoconfigure.base.monitor.MonitorThread;

public class ApplicationPidListener extends ApplicationPidFileWriter {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationPidListener.class);

	private static String DEFAULT_FILE_NAME = "application.pid";

	static {
		String appName = System.getProperty("sun.java.command");
		if (!StringUtil.endsWithIgnoreCase(appName, ".jar")) {
			// String applicationName = StringUtil.remove(appName, ".jar");
			// DEFAULT_FILE_NAME = applicationName + ".pid";
			System.setProperty("application.name", "target/application");
		} else {
			System.setProperty("application.name", "application");
		}
		DEFAULT_FILE_NAME = MessageFormat.format("{0}.pid", System.getProperty("application.name"));
	}

	public ApplicationPidListener() {
		this(new File(DEFAULT_FILE_NAME));
	}

	public ApplicationPidListener(File file) {
		super(file);
	}

	public ApplicationPidListener(String filename) {
		super(filename);
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		super.onApplicationEvent(event);
		if (ApplicationPreparedEvent.class.isInstance(event)) {
			ApplicationPreparedEvent e = (ApplicationPreparedEvent) event;
			ConfigurableEnvironment env = e.getApplicationContext().getEnvironment();
			Timer timer = new Timer(UUID.randomUUID().toString(), true);
			timer.schedule(new MonitorThread(timer, env), 1000, 5000);
			File envFile = new File(MessageFormat.format("{0}-env.json", System.getProperty("application.name")));
			try {
				FileUtil.writeString(envFile, JSON.toJSONString(env, SerializerFeature.PrettyFormat, SerializerFeature.DisableCircularReferenceDetect));
				ResourceListener.addListener(new FileWatchCallbackBean(e.getApplicationContext()));
			} catch (IOException e1) {
				logger.error("写入ENV文件错误", e1);
			} finally {

			}
		}
	}

}

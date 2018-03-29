package td.framework.boot.autoconfigure.base.monitor;

import java.lang.management.ManagementFactory;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;

import jodd.http.HttpRequest;

public class MonitorThread extends TimerTask {

	public final String pid;
	public final ConfigurableEnvironment env;
	public final Timer timer;

	public MonitorThread(Timer timer, ConfigurableEnvironment env) {
		super();
		this.env = env;
		this.pid = this.getPid();
		this.timer = timer;
	}

	private String getPid() {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			return jvmName.split("@")[0];
		} catch (Throwable ex) {
			return null;
		}
	}

	@Override
	public void run() {
		try {
			String devopsServer = StringUtils.trimWhitespace(env.getProperty("devops.server.url"));
			if (devopsServer == null) {
				timer.cancel();
				return;
			}
			String username = StringUtils.trimWhitespace(env.getProperty("devops.server.username"));
			String password = StringUtils.trimWhitespace(env.getProperty("devops.server.password"));
//			String appid = StringUtils.trimWhitespace(env.getProperty("devops.server.appid"));
			HttpRequest httpRequest = HttpRequest.get(devopsServer + "/monitor/collect").charset("UTF-8");
			httpRequest = httpRequest.contentType("application/json", "UTF-8").body(JSON.toJSONString(env)).timeout(1000);
			if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
				httpRequest = httpRequest.basicAuthentication(username, password);
			}
			httpRequest.send();
			// HttpResponse response = httpRequest.send();
			// System.out.println(response.bodyText());
		} catch (Exception e) {
		}
	}

}

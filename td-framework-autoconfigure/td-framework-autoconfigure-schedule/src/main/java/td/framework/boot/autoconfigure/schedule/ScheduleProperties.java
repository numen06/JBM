package td.framework.boot.autoconfigure.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import cn.uncode.schedule.zk.ZKManager.KEYS;

@ConfigurationProperties(prefix = ScheduleProperties.PREFIX, ignoreInvalidFields = true)
public class ScheduleProperties implements InitializingBean {
	public static final String PREFIX = "schedule.zookeeper";

	private String zkConnectString;
	private String rootPath = "/td/schedule";
	private int zkSessionTimeout = 60000;
	private String userName;
	private String password;
	private List<String> ipBlackList;

	private List<String> quartzBean;
	private List<String> quartzMethod;
	private List<String> quartzCronExpression;
	
	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
		// 当DUBBO有zk连接信息的时候，使用DUBBO的设置
		String dubboAddress = environment.getProperty("dubbo.address", "");
		dubboAddress = StringUtils.trimToNull(dubboAddress);
		if (dubboAddress == null)
			return;
		if (!StringUtils.contains(dubboAddress, "zookeeper://"))
			return;
		dubboAddress = StringUtils.lowerCase(dubboAddress);
		dubboAddress = StringUtils.remove(dubboAddress, "zookeeper://");
		if (StringUtils.contains(dubboAddress, "?backup="))
			dubboAddress = StringUtils.replace(dubboAddress, "?backup=", ",");
		this.zkConnectString = dubboAddress;
	}

	public Map<String, String> getConfig() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(KEYS.zkConnectString.key, zkConnectString);
		if (StringUtils.isNotBlank(rootPath)) {
			properties.put(KEYS.rootPath.key, rootPath);
		}
		if (zkSessionTimeout > 0) {
			properties.put(KEYS.zkSessionTimeout.key, zkSessionTimeout + "");
		}
		if (StringUtils.isNotBlank(userName)) {
			properties.put(KEYS.userName.key, userName);
		}
		if (StringUtils.isNotBlank(password)) {
			properties.put(KEYS.password.key, password);
		}
		StringBuilder sb = new StringBuilder();
		if (ipBlackList != null && ipBlackList.size() > 0) {
			for (String ip : ipBlackList) {
				sb.append(ip).append(",");
			}
			ipBlackList.remove(sb.lastIndexOf(","));
		}
		properties.put(KEYS.ipBlacklist.key, sb.toString());
		return properties;
	}

	public String getZkConnectString() {
		return zkConnectString;
	}

	public void setZkConnectString(String zkConnect) {
		this.zkConnectString = zkConnect;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public int getZkSessionTimeout() {
		return zkSessionTimeout;
	}

	public void setZkSessionTimeout(int zkSessionTimeout) {
		this.zkSessionTimeout = zkSessionTimeout;
	}

	public String getUsername() {
		return userName;
	}

	public void setUsername(String zkUsername) {
		this.userName = zkUsername;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String zkPassword) {
		this.password = zkPassword;
	}

	public List<String> getIpBlackList() {
		return ipBlackList;
	}

	public void setIpBlackList(List<String> ipBlackList) {
		this.ipBlackList = ipBlackList;
	}

	public List<String> getQuartzBean() {
		return quartzBean;
	}

	public void setQuartzBean(List<String> quartzBean) {
		this.quartzBean = quartzBean;
	}

	public List<String> getQuartzMethod() {
		return quartzMethod;
	}

	public void setQuartzMethod(List<String> quartzMethod) {
		this.quartzMethod = quartzMethod;
	}

	public List<String> getQuartzCronExpression() {
		return quartzCronExpression;
	}

	public void setQuartzCronExpression(List<String> quartzCronExpression) {
		this.quartzCronExpression = quartzCronExpression;
	}

}

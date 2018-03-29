package td.framework.boot.autoconfigure.level;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.level.LevelOption;

@ConfigurationProperties(prefix = "spring.data.level")
public class LevelProperties extends LevelOption {

	/**
	 * 缓存数据库位置
	 */
	private String cache = "LEVEL_CACHE";

	/**
	 * 是否自动销毁缓存数据库
	 */
	private Boolean autoDestroy = false;

	public String getCache() {
		return cache;
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public Boolean getAutoDestroy() {
		return autoDestroy;
	}

	public void setAutoDestroy(Boolean autoDestroy) {
		this.autoDestroy = autoDestroy;
	}

}
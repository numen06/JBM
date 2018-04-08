package jbm.framework.boot.autoconfigure.dictionary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = DictionaryProperties.PREFIX)
public class DictionaryProperties {

	public static final String PREFIX = "dictionary";

	public final static String DEF_CENTER = "http://dpen.tdenergys.com/td-dpen-center-web/data/dictionary/dictionaryList";

	public final static String DEF_NAMESPACES = "system";
	/**
	 * 操作同步中心
	 */
	private String center = DEF_CENTER;

	/**
	 * 常量扫描
	 */
	private String basepackage;

	private String application = "application";

	public String getCenter() {
		return center;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public String getBasepackage() {
		return basepackage;
	}

	public void setBasepackage(String basepackage) {
		this.basepackage = basepackage;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

}

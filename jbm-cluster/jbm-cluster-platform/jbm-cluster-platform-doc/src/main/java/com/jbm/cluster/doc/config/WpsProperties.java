package com.jbm.cluster.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 属性文件
 * @author wesley.zhang
 */
@Data
@ConfigurationProperties(prefix = "wps")
public class WpsProperties {

	private String domain;
	private String appid;
	private String appsecret;
	private String downloadHost;
	private String localDir;

}

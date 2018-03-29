package com.td.autoconfig.dic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.springframework.data.annotation.Id;

@SuppressWarnings("serial")
public class DictionaryCacheBean implements Serializable {

	// private String key;
	@Id
	private String id;
	private String application;
	private String namespaces = "system";
	private Map<String, String> keyMaps = new HashMap<String, String>();

	public DictionaryCacheBean() {
		super();
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getNamespaces() {
		return namespaces;
	}

	public void setNamespaces(String namespaces) {
		this.namespaces = namespaces;
	}

	public Map<String, String> getKeyMaps() {
		return keyMaps;
	}

	public void setKeyMaps(ConcurrentMap<String, String> keyMaps) {
		this.keyMaps = keyMaps;
	}

	public DictionaryCacheBean put(String key, String value) {
		this.keyMaps.put(key, value);
		return this;
	}

}

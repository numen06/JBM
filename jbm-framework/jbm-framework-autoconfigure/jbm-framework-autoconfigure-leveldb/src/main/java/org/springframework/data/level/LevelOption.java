package org.springframework.data.level;

import org.iq80.leveldb.Options;

public class LevelOption extends Options {

	public LevelOption() {
		super();
		this.cacheSize(100 * 1048576);
	}

	/**
	 * 默认存放目录
	 */
	private String root = "leveldb";

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

}

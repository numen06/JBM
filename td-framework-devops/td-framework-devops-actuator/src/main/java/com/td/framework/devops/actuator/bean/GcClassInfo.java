/**
 * 
 */
package com.td.framework.devops.actuator.bean;

import java.io.Serializable;

/**
 * @author Leonid
 *
 */
public class GcClassInfo implements Serializable {

	private static final long serialVersionUID = -3467445644846911590L;

	/**
	 * 
	 */
	public GcClassInfo() {
		// TODO Auto-generated constructor stub
	}

	// 加载class的数量
	private String Loaded;
	// 所占用空间大小
	private String Loaded_Bytes;
	// 未加载数量
	private String Unloaded;
	// 未加载占用空间
	private String Unloaded_Bytes;
	// 时间
	private String Time;

	public String getLoaded() {
		return Loaded;
	}

	public void setLoaded(String loaded) {
		Loaded = loaded;
	}

	public String getLoaded_Bytes() {
		return Loaded_Bytes;
	}

	public void setLoaded_Bytes(String loaded_Bytes) {
		Loaded_Bytes = loaded_Bytes;
	}

	public String getUnloaded() {
		return Unloaded;
	}

	public void setUnloaded(String unloaded) {
		Unloaded = unloaded;
	}

	public String getUnloaded_Bytes() {
		return Unloaded_Bytes;
	}

	public void setUnloaded_Bytes(String unloaded_Bytes) {
		Unloaded_Bytes = unloaded_Bytes;
	}

	public String getTime() {
		return Time;
	}

	public void setTime(String time) {
		Time = time;
	}

}

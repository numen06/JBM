package com.jbm.util.oshi.info;

import lombok.Data;
/**
 * 类用于表示磁盘空间信息
 */

@Data
public class DiskInfo {

	// 磁盘路径
	private String path;
	// 已使用空间
	private String useSpace;
	// 总空间
	private String totalSpace;
	// 使用百分比
	private String percent;


}

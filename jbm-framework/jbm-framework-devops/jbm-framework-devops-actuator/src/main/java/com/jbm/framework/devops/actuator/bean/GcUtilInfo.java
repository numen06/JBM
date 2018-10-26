/**
 * 
 */
package com.jbm.framework.devops.actuator.bean;


/**
 * @author Leonid
 *
 */
public interface GcUtilInfo {

	// 幸存1区当前使用比例
	public final static String S0 = "S0";
	// 幸存2区当前使用比例
	public final static String S1 = "S1";
	// 伊甸园区使用比例
	public final static String E = "E";
	// 老年代使用比例
	public final static String O = "O";
	// 元数据区使用比例
	public final static String M = "M";
	// 压缩使用比例
	public final static String CCS = "CCS";
	// 年轻代垃圾回收次数
	public final static String YGC = "YGC";
	// 老年代垃圾回收次数
	public final static String FGC = "FGC";
	// 老年代垃圾回收消耗时间
	public final static String FGCT = "FGCT";
	// 垃圾回收消耗总时间
	public final static String GCT = "GCT";

}

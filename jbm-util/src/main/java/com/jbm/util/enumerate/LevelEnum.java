/* @()LogSupportEnum.java
 *
 * (c) COPYRIGHT 1998-2010 Newcosoft INC. All rights reserved.
 * Newcosoft CONFIDENTIAL PROPRIETARY
 * Newcosoft Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author             Date                   Brief Description
 * -----------------  ----------     ---------------------------------------
 * linto            上午10:28:57                init version
 * 
 */
package com.jbm.util.enumerate;

/**
 * 两个之间作为一个区间,前开后闭如5<x<=10,其中null也为0
 * 暂时定为五个级别
 * 如:0-0,0-5,5-10,10-20,20-100
 * @author linto
 *
 */
public enum LevelEnum {

	/**
	 * 逆变器
	 */
	invLevel(new Integer[]{0,0,0,5,5,10,10,20,20,100}),
	/**
	 * 汇流箱
	 */
	comBoxLevel(new Integer[]{0,0,0,5,5,10,10,20,20,100}),
	/**
	 * 组串
	 */
	stringLevel(new Integer[]{0,0,0,5,5,10,10,20,20,100}),
	/**
	 * 汇集线
	 */
	collectLine(new Integer[]{0,0,0,5,5,10,10,20,20,100});

	private Integer range[];

	private LevelEnum(Integer range[]) {
		this.range = range;
	}

	public Integer[] getRange() {
		return range;
	}

	public void setRange(Integer[] range) {
		this.range = range;
	}

	

}

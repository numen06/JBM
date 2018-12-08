package com.jbm.framework.constant;

import com.alibaba.fastjson.JSON;

/**
 * @author wesley.zhang
 *
 */
public class ConstantDictionary {

	/**
	 * 编码
	 */
	private final String code;
	/**
	 * 值
	 */
	private String value;
	/**
	 * 描述
	 */
	private String desc;

	public ConstantDictionary(String code) {
		super();
		this.code = code;
	}

	public ConstantDictionary(String code, String value) {
		this.code = code;
		this.value = value;
	}

	public ConstantDictionary(String code, String value, String desc) {
		super();
		this.code = code;
		this.value = value;
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public String getValue() {
		return value;
	}

	public String getDesc() {
		return desc;
	}

	public String toJsonString() {
		return JSON.toJSONString(this);
	}

	public boolean eq(String code) {
		return this.code.equals(code);
	}

	@Override
	public String toString() {
		return "code:" + code + ";" + "value:" + value + ";" + "desc:" + desc + ";";
	}

	public static ConstantDictionary valueOf(String code) {
		return new ConstantDictionary(code);
	}

	public static ConstantDictionary valueOf(String code, String value) {
		return new ConstantDictionary(code, value);
	}

	public static ConstantDictionary valueOf(String code, String value, String desc) {
		return new ConstantDictionary(code, value, desc);
	}
}

package com.jbm.dic.test;

import com.jbm.framework.constant.Constant;
import com.jbm.framework.constant.ConstantDictionary;

@Constant
public interface DicTestBean3 {
	public static final ConstantDictionary test0 = ConstantDictionary.valueOf("0000");
	public static final ConstantDictionary test1 = ConstantDictionary.valueOf("0001", "测试1");
	public static final ConstantDictionary test2 = ConstantDictionary.valueOf("0002", "测试2");
	public static final ConstantDictionary test3 = ConstantDictionary.valueOf("0003", "测试3", "描述3");
	public static final ConstantDictionary test4 = ConstantDictionary.valueOf("0004", "测试4", "描述4");

}

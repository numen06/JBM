package com.td.test;

import com.alibaba.fastjson.JSON;

import moudles.Student;
import td.framework.excel.annotation.ExcelField;
import td.framework.excel.handler.ExcelWapper;
import td.framework.excel.utils.Utils;

public class HeadTest {
	public class test extends ExcelWapper<Student> {
		@ExcelField(title = "学号", order = 1)
		String id;
		// 姓名
		@ExcelField(title = "姓名", order = 2)
		String name;
		// 班级
		@ExcelField(title = "班级", order = 3)
		String classes;
	}

	public static void main(String[] args) {
		System.out.println(JSON.toJSONString(Utils.getHeaderList(test.class)));
	}
}

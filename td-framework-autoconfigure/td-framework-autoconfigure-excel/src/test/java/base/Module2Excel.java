package base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import moudles.Student1;
import td.framework.excel.ExcelTemplate;

public class Module2Excel {

	@Test
	public void testObject2Excel() throws Exception {

		String tempPath = "D:\\IdeaSpace\\Excel4J\\src\\test\\resource\\normal_template.xlsx";
		List<Student1> list = new ArrayList<>();
		list.add(new Student1("1010001", "盖伦", "六年级三班"));
		list.add(new Student1("1010002", "古尔丹", "一年级三班"));
		list.add(new Student1("1010003", "蒙多(被开除了)", "六年级一班"));
		list.add(new Student1("1010004", "萝卜特", "三年级二班"));
		list.add(new Student1("1010005", "奥拉基", "三年级二班"));
		list.add(new Student1("1010006", "得嘞", "四年级二班"));
		list.add(new Student1("1010007", "瓜娃子", "五年级一班"));
		list.add(new Student1("1010008", "战三", "二年级一班"));
		list.add(new Student1("1010009", "李四", "一年级一班"));
		Map<String, String> data = new HashMap<>();
		data.put("title", "战争学院花名册");
		data.put("info", "学校统一花名册");
		// 基于模板导出Excel
		ExcelTemplate.getInstance().exportObjects2Excel(tempPath, 0, list, data, Student1.class, false, "D:/A.xlsx");
		// 不基于模板导出Excel
		ExcelTemplate.getInstance().exportObjects2Excel(list, Student1.class, true, null, true, "D:/B.xlsx");

	}

	@Test
	public void testMap2Excel() throws Exception {

		Map<String, List> classes = new HashMap<>();

		Map<String, String> data = new HashMap<>();
		data.put("title", "战争学院花名册");
		data.put("info", "学校统一花名册");

		classes.put("class_one", new ArrayList<Student1>() {
			{
				add(new Student1("1010009", "李四", "一年级一班"));
				add(new Student1("1010002", "古尔丹", "一年级三班"));
			}
		});
		classes.put("class_two", new ArrayList<Student1>() {
			{
				add(new Student1("1010008", "战三", "二年级一班"));
			}
		});
		classes.put("class_three", new ArrayList<Student1>() {
			{
				add(new Student1("1010004", "萝卜特", "三年级二班"));
				add(new Student1("1010005", "奥拉基", "三年级二班"));
			}
		});
		classes.put("class_four", new ArrayList<Student1>() {
			{
				add(new Student1("1010006", "得嘞", "四年级二班"));
			}
		});
		classes.put("class_six", new ArrayList<Student1>() {
			{
				add(new Student1("1010001", "盖伦", "六年级三班"));
				add(new Student1("1010003", "蒙多", "六年级一班"));
			}
		});

		// ExcelTemplate.getInstance().exportObject2Excel("D:\\IdeaSpace\\Excel4J\\src\\test\\resource\\map_template.xlsx",
		// 0, classes, data, Student1.class, false, "D:/C.xlsx");
	}

	@Test
	public void testList2Excel() throws Exception {

		List<List<String>> list2 = new ArrayList<>();
		List<String> header = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			List<String> _list = new ArrayList<>();
			for (int j = 0; j < 10; j++) {
				_list.add(i + " -- " + j);
			}
			list2.add(_list);
			header.add(i + "---");
		}
//		ExcelTemplate.getInstance().exportObjects2Excel(list2, header, "D:/D.xlsx");
	}
}

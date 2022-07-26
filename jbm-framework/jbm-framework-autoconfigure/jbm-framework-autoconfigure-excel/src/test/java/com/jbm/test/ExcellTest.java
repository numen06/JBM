package com.jbm.test;

import jbm.framework.boot.autoconfigure.excel.ExcelAutoConfiguration;
import jbm.framework.excel.ExcelTemplate;
import moudles.Student;
import moudles.Student1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
// @Import(BeetlProperties.class)
@SpringBootTest(classes = ExcelAutoConfiguration.class)
public class ExcellTest {

    @Autowired
    private ExcelTemplate excelTemplate;

    @Test
    public void exampleMapTest() throws Exception {
        List<Student1> list = excelTemplate.readExcel2Objects("A.xlsx", Student1.class);
        excelTemplate.exportObjects2Excel(list, Student1.class, true, "B.xlsx");
    }

    // @Test
    public void exampleTest() throws Exception {
        String tempPath = "classpath:temp.xlsx";
        List<Student> list = new ArrayList<>();
        list.add(new Student("1010001", "盖伦", "六年级三班"));
        list.add(new Student("1010002", "古尔丹", "一年级三班"));
        list.add(new Student("1010003", "蒙多(被开除了)", "六年级一班"));
        list.add(new Student("1010004", "萝卜特", "三年级二班"));
        list.add(new Student("1010005", "奥拉基", "三年级二班"));
        list.add(new Student("1010006", "得嘞", "四年级二班"));
        list.add(new Student("1010007", "瓜娃子", "五年级一班"));
        list.add(new Student("1010008", "战三", "二年级一班"));
        list.add(new Student("1010009", "李四", "一年级一班"));
        Map<String, String> data = new HashMap<>();
        data.put("title", "战争学院花名册");
        data.put("info", "学校统一花名册");
        // 基于模板导出Excel
        excelTemplate.exportObjects2Excel(list, Student.class, true, "A.xlsx");

        // 基于模板导出Excel
        // excelTemplate.exportObjects2Excel(tempPath, 0, list, result,
        // Student.class, false, "B.xlsx");
        // 不基于模板导出Excel
    }

}

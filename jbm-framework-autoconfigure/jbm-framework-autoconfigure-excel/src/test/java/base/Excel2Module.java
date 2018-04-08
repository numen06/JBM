package base;

import java.util.List;

import org.junit.Test;

import moudles.Student1;
import moudles.Student2;
import jbm.framework.excel.ExcelTemplate;

public class Excel2Module {

    @Test
    public void excel2Object() throws Exception {
        String path = "D:\\IdeaSpace\\Excel4J\\src\\test\\resource\\students_01.xlsx";

        System.out.println("读取全部：");
        List<Student1> students = ExcelTemplate.getInstance().readExcel2Objects(path, Student1.class);
        for (Student1 stu : students) {
            System.out.println(stu);
        }

        System.out.println("读取指定行数：");
        students = ExcelTemplate.getInstance().readExcel2Objects(path, Student1.class, 0, 3, 0);
        for (Student1 stu : students) {
            System.out.println(stu);
        }
    }

    @Test
    public void excel2Object2() throws Exception {
        
        String path = "D:\\IdeaSpace\\Excel4J\\src\\test\\resource\\students_02.xlsx";

        // 不基于注解,将Excel内容读至List<List<String>>对象内
        List<List<String>> lists = ExcelTemplate.getInstance().readExcel2List(path, 1, 3, 0);
        System.out.println("读取Excel至String数组：");
        for (List<String> list : lists) {
            System.out.println(list);
        }
        // 基于注解,将Excel内容读至List<Student2>对象内
        List<Student2> students = ExcelTemplate.getInstance().readExcel2Objects(path, Student2.class, 0);
        System.out.println("读取Excel至对象数组(支持类型转换)：");
        for (Student2 st : students) {
            System.out.println(st);
        }
    }
}

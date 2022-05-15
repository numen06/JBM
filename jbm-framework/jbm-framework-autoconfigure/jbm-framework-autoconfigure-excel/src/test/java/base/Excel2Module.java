package base;

import jbm.framework.excel.ExcelTemplate;
import jbm.framework.excel.model.ReadParam;
import moudles.Student1;
import moudles.Student2;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        ReadParam readParam = new ReadParam(0, 3, 0);
        students = ExcelTemplate.getInstance().readExcel2Objects(path, Student1.class, readParam);
        for (Student1 stu : students) {
            System.out.println(stu);
        }
    }

    @Test
    public void excel2Object2() throws Exception {

        String path = "D:\\IdeaSpace\\Excel4J\\src\\test\\resource\\students_02.xlsx";

        // 不基于注解,将Excel内容读至List<List<String>>对象内
        List<List<String>> lists = ExcelTemplate.getInstance().readExcel2List(path,new ReadParam(1, 3, 0));
        System.out.println("读取Excel至String数组：");
        for (List<String> list : lists) {
            System.out.println(list);
        }
        // 基于注解,将Excel内容读至List<Student2>对象内
        List<Student2> students = ExcelTemplate.getInstance().readExcel2Objects(path, Student2.class);
        System.out.println("读取Excel至对象数组(支持类型转换)：");
        for (Student2 st : students) {
            System.out.println(st);
        }
    }
}

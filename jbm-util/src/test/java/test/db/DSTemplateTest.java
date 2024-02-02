package test.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapUtil;
import cn.hutool.db.Page;
import com.alibaba.fastjson.JSON;
import com.jbm.util.db.DSTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import test.entity.GradeResult;
import test.entity.Student;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @create 2020-06-22 16:03
 */
@Slf4j
public class DSTemplateTest {


    private DSTemplate dst;

    @Before
    public void setUp() {
        dst = new DSTemplate("test");
        dst.execute("create-db");
        dst.execute("db-init");
    }

    @Test
    public void initStudent() {
        dst.insertForGeneratedKey(new Student("随机2", 12, DateTime.now()));

        Long id = dst.insertForGeneratedKey(new Student(DateTime.now().getTime(), "随机1", 12, DateTime.now()));

        List<Student> students = dst.find(new Student());

        log.info("插入结果:{}", JSON.toJSONString(students));


        dst.update(new Student(id, "随机123", 12, DateTime.now()), "id");
        students = dst.find(new Student());
        log.info("更新结果:{}", JSON.toJSONString(students));

        dst.delete(new Student(id), "id");

        students = dst.find(new Student());
        log.info("删除结果:{}", JSON.toJSONString(students));


    }

    @Test
    public void testPageEntitys() {
        String sqlName = "page";
        log.info("开始查询:{}", sqlName);
        List<Student> students = dst.page("page", Student.class, new Page(0, 3), "张三");
        log.info("分页查询结果[{}]:{}", sqlName, students);
    }


    @Test
    public void testQueryEntitys() {
        String sqlName = "test";
        log.info("开始查询:{}", sqlName);
        List<Student> students = dst.queryEntitys("test", Student.class, "张三");
        log.info("查询结果[{}]:{}", sqlName, students);
    }

    @Test
    public void testQueryEntitys2() {
        String sqlName = "test-map.selectByFileName";
        log.info("开始查询:{}", sqlName);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("list", CollUtil.newArrayList("张三", "李四", "王五"));
        List<Student> students = dst.queryEntitys(sqlName, Student.class, map);
        log.info("查询结果[{}]:{}", sqlName, students);
    }

    @Test
    public void testQueryEntitys3() {
        String sqlName = "test-bean";
        log.info("开始查询:{}", sqlName);
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new Date());
        List<Student> students = dst.queryEntitys("test-bean", Student.class, student);
        log.info("查询结果[{}]:{}", sqlName, students);
    }

    @Test
    public void testQueryEntitys4() {
        String sqlName = "test-map.selectByFileName2";
        log.info("开始查询:{}", sqlName);
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new Date());
        Map<String, Object> map = MapUtil.of("list", CollUtil.newArrayList(student));
        List<Student> students = dst.queryEntitys(sqlName, Student.class, map);
        log.info("查询结果[{}]:{}", sqlName, students);
    }

    @Test
    public void testQueryEntitys5() {
        String sqlName = "test-map.selectByFileName3";
        log.info("开始查询:{}", sqlName);
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new Date());
        List<Student> students = dst.queryEntitys(sqlName, Student.class, student);
        log.info("查询结果[{}]:{}", sqlName, students);
    }

    @Test
    public void testQueryEntitys6() {
        String sqlName = "test-map.selectByFileName4";
        log.info("开始查询:{}", sqlName);
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new Date());
        GradeResult gradeResult = new GradeResult();
        gradeResult.setStudent(student);
        List<Student> students = dst.queryEntitys("test-map.selectByFileName4", Student.class, gradeResult);
        log.info("查询结果[{}]:{}", sqlName, students);
    }
}
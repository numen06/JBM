package test.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.jbm.util.db.DSTemplate;
import org.junit.Before;
import org.junit.Test;
import test.entity.GradeResult;
import test.entity.SqlInitialize;
import test.entity.Student;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @create 2020-06-22 16:03
 *
 */
public class DSTemplateTest {


    private DSTemplate dst;

    @Before
    public void setUp() {
        dst = new DSTemplate("test");
    }

    @Test
    public void testQueryEntitys() {
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test", SqlInitialize.class);
        System.out.println(sqlInitializers);
    }

    @Test
    public void testQueryEntitys2() {
        Map<String,Object> map= new LinkedHashMap<>();
        map.put("list", CollUtil.newArrayList("123","223","333"));
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test-map.selectByFileName", SqlInitialize.class,map);
        System.out.println(sqlInitializers);
    }

    @Test
    public void testQueryEntitys3() {
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new java.util.Date());
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test-bean", SqlInitialize.class,student);
        System.out.println(sqlInitializers);
    }

    @Test
    public void testQueryEntitys4() {
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new java.util.Date());
        Map<String,Object> map = MapUtil.of("list",CollUtil.newArrayList(student));
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test-map.selectByFileName2", SqlInitialize.class, map);
        System.out.println(sqlInitializers);
    }
    @Test
    public void testQueryEntitys5() {
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new java.util.Date());
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test-map.selectByFileName3", SqlInitialize.class, student);
        System.out.println(sqlInitializers);
    }

    @Test
    public void testQueryEntitys6() {
        Student student = new Student();
        student.setAge(12);
        student.setName("张三");
        student.setTime(new java.util.Date());
        GradeResult gradeResult = new GradeResult();
        gradeResult.setStudent(student);
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test-map.selectByFileName4", SqlInitialize.class, gradeResult);
        System.out.println(sqlInitializers);
    }
}
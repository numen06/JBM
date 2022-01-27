package test.proxy;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSON;
import com.jbm.util.proxy.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.ann.TestRequest;
import test.entity.Student;
import test.service.IStudentTest;
import test.service.impl.StudentTest;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
public class ProxyTest {

    @Test
    public void findAnnMethods() {
        List<Method> methods = ReflectUtils.findAnnotationMethods(IStudentTest.class, TestRequest.class);
        log.info(JSON.toJSONString(methods));
        Student student = new Student();
        student.setAge(1);
        student.setName("张三");
        student.setTime(DateTime.now());
        String json = JSON.toJSONString(student);
        StudentTest studentTest = new StudentTest();
        for (Method method : methods) {
            Object result = ReflectUtils.invokeMethodFromJsonData(studentTest, method, json);
            log.info(JSON.toJSONString(result));
        }
    }
}

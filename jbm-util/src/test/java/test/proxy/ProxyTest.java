package test.proxy;

import cn.hutool.aop.ProxyUtil;
import cn.hutool.aop.aspects.TimeIntervalAspect;
import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.jbm.util.proxy.ReflectUtils;
import com.jbm.util.proxy.wapper.RequestHeaders;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.ann.TestRequest;
import test.entity.ClassRoom;
import test.entity.Student;
import test.service.IStudentTest;
import test.service.impl.StudentTest;

import java.lang.reflect.Method;
import java.util.Date;
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

        ClassRoom classRoom = new ClassRoom();
        classRoom.setRoomName("三年一班");
        classRoom.setGrade(3);


        String json = JSON.toJSONString(ImmutableMap.of("student", student, "classRoom", classRoom));
        StudentTest studentTest = new StudentTest();
        RequestHeaders requestHeader = new RequestHeaders();
        requestHeader.set("requestTime", DateTime.now());
        for (Method method : methods) {
            Object result = ReflectUtils.invokeMethodFromJsonData(studentTest, method, json, requestHeader);
            log.info("method:{},reslut:{}", method, JSON.toJSONString(result));
        }
    }

    @Test
    public void invikeAnnMethods() {
        Student student = new Student();
        student.setAge(1);
        student.setName("张三");
        student.setTime(DateTime.now());
        StudentTest studentTest = new StudentTest();
        studentTest = ProxyUtil.proxy(studentTest, new TimeIntervalAspect());
        studentTest.exam(student, null

                , null);
    }
}

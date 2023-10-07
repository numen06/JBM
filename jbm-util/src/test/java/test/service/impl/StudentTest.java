package test.service.impl;

import com.alibaba.fastjson.JSON;
import com.jbm.util.proxy.annotation.RequestBody;
import com.jbm.util.proxy.annotation.RequestHeader;
import com.jbm.util.proxy.annotation.RequestParam;
import com.jbm.util.proxy.wapper.RequestHeaders;
import lombok.extern.slf4j.Slf4j;
import test.entity.ClassRoom;
import test.entity.GradeResult;
import test.entity.Student;
import test.service.IStudentTest;

@Slf4j
public class StudentTest implements IStudentTest {
    @Override
    public GradeResult exam() {
        return null;
    }

    @Override
    public GradeResult exam(@RequestParam("student") Student student, @RequestHeader("requestTime") String requestTime, RequestHeaders requestHeaders) {
        log.info("requestTime:{}", requestTime);
        GradeResult result = new GradeResult();
        result.setStudent(student);
        return result;
    }

    @Override
    public GradeResult exam(Student student, ClassRoom classRoom) {
//        if (ObjectUtil.isNotEmpty(student)) {
//            student.setTime(time);
//        }
        GradeResult result = new GradeResult();
        result.setClassRoom(classRoom);
        result.setStudent(student);
        return result;
    }

    @Override
    public GradeResult exam(@RequestBody String body) {
        GradeResult result = JSON.parseObject(body, GradeResult.class);
        return result;
    }
}

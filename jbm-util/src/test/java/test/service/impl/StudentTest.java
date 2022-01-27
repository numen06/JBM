package test.service.impl;

import cn.hutool.core.util.ObjectUtil;
import test.entity.GradeResult;
import test.entity.Student;
import test.service.IStudentTest;

import java.util.Date;

public class StudentTest implements IStudentTest {
    @Override
    public GradeResult exam() {
        return null;
    }

    @Override
    public GradeResult exam(Student student) {
        GradeResult result = new GradeResult();
        result.setStudent(student);
        return result;
    }

    @Override
    public GradeResult exam(Student student, Date time) {
        if (ObjectUtil.isNotEmpty(student)) {
            student.setTime(time);
        }
        GradeResult result = new GradeResult();
        result.setStudent(student);
        return result;
    }
}

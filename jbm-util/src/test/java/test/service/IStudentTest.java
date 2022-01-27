package test.service;

import lombok.Data;
import test.ann.TestRequest;
import test.entity.GradeResult;
import test.entity.Student;

import java.util.Date;

public interface IStudentTest {

    /**
     * 考试
     *
     * @return
     */
    @TestRequest(formTopic = "/from", toTopic = "/to")
    GradeResult exam();

    @TestRequest(formTopic = "/from", toTopic = "/to")
    GradeResult exam(Student student);

    @TestRequest(formTopic = "/from", toTopic = "/to")
    GradeResult exam(Student student, Date time);
}

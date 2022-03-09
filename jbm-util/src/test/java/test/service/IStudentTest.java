package test.service;

import com.jbm.util.proxy.annotation.RequestBody;
import com.jbm.util.proxy.annotation.RequestHeader;
import com.jbm.util.proxy.annotation.RequestParam;
import com.jbm.util.proxy.wapper.RequestHeaders;
import test.ann.TestRequest;
import test.entity.ClassRoom;
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
    GradeResult exam(@RequestParam("student") Student student, @RequestHeader("requestTime") String requestTime, RequestHeaders requestHeaders);

    @TestRequest(formTopic = "/from", toTopic = "/to")
    GradeResult exam(@RequestParam("student") Student student, @RequestParam("classRoom") ClassRoom classRoom);

    @TestRequest(formTopic = "/from", toTopic = "/to")
    GradeResult exam(@RequestBody String body);
}

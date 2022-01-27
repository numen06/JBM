package test.entity;

import lombok.Data;

/**
 * 考试结果
 */
@Data
public class GradeResult {

    /**
     * 同学
     */
    private Student student;

    /**
     * 成绩
     */
    private Double score;
}

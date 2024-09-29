package test.entity;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private Integer age;

    private Date time;


    public Student() {
    }

    public Student(Date time) {
        this.time = time;
    }

    public Student(String name, Integer age, Date time) {
        this.name = name;
        this.age = age;
        this.time = time;
    }


    public Student(Long id, String name, Integer age, Date time) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.time = time;
    }

    public Student(Long id) {
        this.id = id;
    }

    public static Student newStudent() {
        return new Student(IdUtil.getSnowflakeNextId(), "测试", RandomUtil.randomInt(10, 100), new DateTime());
    }
}

package test.entity;

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
}

package moudles;

import java.util.Date;

public class Student2 {

    private Long id;

    private String name;

    private Date date;

    private Integer classes;

    private String expel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Integer getClasses() {
        return classes;
    }

    public void setClasses(Integer classes) {
        this.classes = classes;
    }

    public String getExpel() {
        return expel;
    }

    public void setExpel(String expel) {
        this.expel = expel;
    }

    @Override
    public String toString() {
        return "Student2{" + "id=" + id + ", name='" + name + '\'' + ", date=" + date + ", classes=" + classes + ", expel='" + expel + '\'' + '}';
    }
}

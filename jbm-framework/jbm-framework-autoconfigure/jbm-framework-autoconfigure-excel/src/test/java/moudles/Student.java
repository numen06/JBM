package moudles;

public class Student {

    // 学号
    private String id;

    // 姓名
    private String name;

    // 班级
    private String classes;

    public Student() {

    }

    public Student(String id, String name, String classes) {
        this.id = id;
        this.name = name;
        this.classes = classes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    @Override
    public String toString() {
        return "Student1{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", classes='" + classes + '\'' + '}';
    }
}

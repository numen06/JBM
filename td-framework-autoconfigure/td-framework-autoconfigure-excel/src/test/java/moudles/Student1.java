package moudles;

import java.util.Date;

import td.framework.excel.annotation.ExcelField;

public class Student1 {

	// 学号
	@ExcelField(title = "学号", order = 1)
	private String id;

	// 姓名
	@ExcelField(title = "姓名", order = 2)
	private String name;

	// 班级
	@ExcelField(title = "班级", order = 3)
	private String classes;

	@ExcelField(title = "分数", order = 4)
	private Long code;
	@ExcelField(title = "创建时间", order = 5, format="yyyy-MM-dd")
	private Date createTime;

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

	public Student1() {

	}

	public Student1(String id, String name, String classes) {
		this.id = id;
		this.name = name;
		this.classes = classes;
	}

	public Long getCode() {
		return code;
	}

	public void setCode(Long code) {
		this.code = code;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Override
	public String toString() {
		return "Student1{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", classes='" + classes + '\'' + '}';
	}
}

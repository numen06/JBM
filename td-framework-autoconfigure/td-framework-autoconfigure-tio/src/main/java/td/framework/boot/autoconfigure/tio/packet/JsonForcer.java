package td.framework.boot.autoconfigure.tio.packet;

import java.io.Serializable;
import java.util.Date;

/**
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 */
public class JsonForcer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -448644988609245848L;

	private Class<?> clazz;

	private Object target;

	/**
	 * 0:心跳包，1:正常消息
	 */
	private Integer type = 1;

	private Date createTime = new Date();

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public JsonForcer() {
		super();
	}

	public JsonForcer(Object target) {
		super();
		this.target = target;
	}

	public JsonForcer(Object target, Integer type) {
		super();
		this.target = target;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public <T> T toJavaBean() {
		return (T) this.target;
	}

}

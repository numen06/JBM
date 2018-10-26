package jbm.framework.boot.autoconfigure.tio.packet;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.JSON;

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

	private Class<?> targetClass;

	private String targetSource;

	/**
	 * 0:心跳包，1:正常消息
	 */
	private Integer type = 1;

	private Date createTime = new Date();

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
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

	public String getTargetSource() {
		return targetSource;
	}

	public void setTargetSource(String targetSource) {
		this.targetSource = targetSource;
	}

	@SuppressWarnings("unchecked")
	public <T> T getTarget() {
		return (T) JSON.parseObject(this.targetSource, targetClass);
	}

	public JsonForcer() {
		super();
	}

	public JsonForcer(Object target) {
		super();
		this.targetSource = JSON.toJSONString(target);
		if (target != null)
			this.targetClass = target.getClass();
	}

	public JsonForcer(Object target, Integer type) {
		super();
		this.targetSource = JSON.toJSONString(target);
		this.type = type;
		if (target != null)
			this.targetClass = target.getClass();
	}

	// public T toJavaBean(Class clazz) {
	// return JSON.parseObject(target, clazz);
	// }

}

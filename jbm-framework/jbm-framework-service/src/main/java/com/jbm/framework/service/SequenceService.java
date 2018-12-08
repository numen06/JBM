package com.jbm.framework.service;

import java.util.Collection;

import com.jbm.framework.metadata.bean.ClsInfo;
import com.jbm.framework.metadata.bean.Sequence;

/**
 * Created with IntelliJ IDEA. User: Joe Xie Date: 14-7-31 Time: 上午9:16
 */
public interface SequenceService {

	/**
	 * 保存序列号生成器（更新or添加）
	 * 
	 * @param record
	 *            序列记录
	 */
	public void save(Sequence record);

	/**
	 * 获取最后一个被使用的序列号
	 * 
	 * @param tableName
	 *            表名
	 * @return 返回
	 */
	public Long getLastSequence(String tableName);

	/**
	 * 根据表名来获取下一个序列号
	 * 
	 * @param tableName
	 *            表名
	 * @return 序列号
	 */
	// public Long nextSequence(String tableName);

	/**
	 * @deprecated
	 * 
	 *             根据表名以及其主键来获取序列号， 作用有别于上一种，在首次进行获取序列号操作后会自动获取目前使用的最大值并持续递增
	 * 
	 * @param tableName
	 *            表名
	 * @param pk
	 *            主键字段
	 * @return 序列号
	 */
	// public Long nextSequence(String tableName, String pk);

	/**
	 * 获取一组序列号，根据表名以及需要的序列号大小来获取一个数组
	 * 
	 * @param tableName
	 *            表名
	 * @param size
	 *            获取的个数
	 * @return 序列号数组
	 */
	// public Long[] nextSequences(String tableName, int size);

	/**
	 * 根据表名以及主键来获取一组序列号
	 * 
	 * @param tableName
	 *            表名
	 * @param pk
	 *            主键
	 * @param size
	 *            规模
	 * @return 序列号数组
	 */
	// public Long[] nextSequences(String tableName, String pk, int size);

	/**
	 * 为某一个数据POJO注入系统生成的LONG型主键
	 * 
	 * @param entity
	 *            实体对象
	 * @param <T>
	 *            泛型
	 * @return 返回生成的对象
	 */
	public <T> T putPK(T entity);

	/**
	 * 为某一组数据注入系统生成的LONG型主键
	 * 
	 * @param entities
	 *            实体对象组
	 * @param <T>
	 *            泛型
	 * @return 已经注入的对象组
	 */
	public <T> Collection<T> putPK(Collection<T> entities);

	/**
	 * 校验ID是否正确
	 */
	void checkLastSequence();

	/**
	 * 校验指定ID是否正确
	 * 
	 * @param clsInfo
	 */
	void checkLastSequence(ClsInfo clsInfo);

}

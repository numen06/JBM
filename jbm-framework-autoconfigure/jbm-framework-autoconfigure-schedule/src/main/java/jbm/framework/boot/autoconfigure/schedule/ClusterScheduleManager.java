package jbm.framework.boot.autoconfigure.schedule;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.uncode.schedule.ConsoleManager;
import cn.uncode.schedule.ZKScheduleManager;
import cn.uncode.schedule.core.TaskDefine;

/**
 * @author wesley.zhang
 *
 */
public class ClusterScheduleManager {

	private static transient Logger logger = LoggerFactory.getLogger(ClusterScheduleManager.class);

	protected ZKScheduleManager zKScheduleManager;

	public ClusterScheduleManager() {
		super();
	}

	public ClusterScheduleManager(ZKScheduleManager zKScheduleManager) {
		super();
		this.zKScheduleManager = zKScheduleManager;
		try {
			while (ConsoleManager.getScheduleManager().getScheduleDataManager() == null) {
				Thread.sleep(500);
			}
		} catch (Exception e) {
			logger.error("初始化分布式任务失败", e);
		}
	}

	public TaskDefine createTaskFromCron(Object targetBean, String targetMethod, String cronExpression, Date startTime, String params) {
		String[] beanNames = ZKScheduleManager.getApplicationcontext().getBeanNamesForType(targetBean.getClass());
		TaskDefine task = new TaskDefine();
		task.setTargetBean(beanNames[0]);
		task.setCronExpression(cronExpression);
		task.setTargetMethod(targetMethod);
		if (startTime == null)
			task.setStartTime(new Date());
		else
			task.setStartTime(startTime);
		task.setTargetMethod(targetMethod);
		task.setParams(params);
		return task;
	}

	public TaskDefine createTaskFromPeriod(Object targetBean, String targetMethod, long period, Date startTime, String params) {
		String[] beanNames = ZKScheduleManager.getApplicationcontext().getBeanNamesForType(targetBean.getClass());
		TaskDefine task = new TaskDefine();
		task.setTargetBean(beanNames[0]);
		task.setPeriod(period);
		task.setTargetMethod(targetMethod);
		if (startTime == null)
			task.setStartTime(new Date());
		else
			task.setStartTime(startTime);
		task.setTargetMethod(targetMethod);
		task.setParams(params);
		return task;
	}

	public void addOrUpdateScheduleTask(TaskDefine taskDefine) {
		try {
			if (ConsoleManager.isExistsTask(taskDefine)) {
				this.delScheduleTask(taskDefine);
			}
		} catch (Exception e) {
		}
		this.addScheduleTask(taskDefine);

	}

	/**
	 * 动态添加分布式任务调度
	 * 
	 * @param taskDefine
	 */
	public void addScheduleTask(TaskDefine taskDefine) {
		if (StringUtils.isBlank(taskDefine.getTargetMethod())) {
			throw new NullPointerException("method is null");
		}
		if (StringUtils.contains(taskDefine.getTargetMethod(), "-")) {
			taskDefine.setTargetMethod(StringUtils.substringBefore(taskDefine.getTargetMethod(), "-"));
		}
		ConsoleManager.addScheduleTask(taskDefine);
	}

	/**
	 * 动态删除分布式任务调度
	 * 
	 * @param taskDefine
	 */
	public void delScheduleTask(TaskDefine taskDefine) {
		if (StringUtils.contains(taskDefine.getTargetMethod(), "-")) {
			taskDefine.setTargetMethod(StringUtils.substringBefore(taskDefine.getTargetMethod(), "-"));
		}
		ConsoleManager.delScheduleTask(taskDefine);
	}

	/**
	 * 动态删除分布式任务调度
	 * 
	 * @param targetBean
	 * @param targetMethod
	 */
	public void delScheduleTask(String targetBean, String targetMethod) {
		ConsoleManager.delScheduleTask(targetBean, targetMethod);
	}

	/**
	 * 查询动态任务清单
	 * 
	 * @return
	 */
	public List<TaskDefine> queryScheduleTask() {
		return ConsoleManager.queryScheduleTask();
	}

	/**
	 * 清理任务
	 */
	public void clearScheduleTask() {
		List<TaskDefine> defines = this.queryScheduleTask();
		for (TaskDefine d : defines) {
			this.delScheduleTask(d);
		}
	}

	/**
	 * 清空没有运行的任务
	 */
	public void clearUnRunScheduleTask() {
		List<TaskDefine> defines = this.queryScheduleTask();
		for (TaskDefine d : defines) {
			if (!"running".equals(d.getStatus())) {
				this.delScheduleTask(d);
			}
		}
	}

	/**
	 * 更新信息
	 * 
	 * @param taskDefine
	 */
	public void updateScheduleTask(TaskDefine taskDefine) {
		ConsoleManager.updateScheduleTask(taskDefine);
	}

	/**
	 * 判断是否存在动态任务
	 * 
	 * @param taskDefine
	 * @return
	 * @throws Exception
	 */
	public boolean isExistsTask(TaskDefine taskDefine) throws Exception {
		return ConsoleManager.isExistsTask(taskDefine);
	}

}

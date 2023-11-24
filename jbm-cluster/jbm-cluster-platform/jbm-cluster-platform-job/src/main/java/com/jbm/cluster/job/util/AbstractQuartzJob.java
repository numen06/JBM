package com.jbm.cluster.job.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.jbm.cluster.api.constants.job.ScheduleConstants;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.api.entitys.job.SysJobLog;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.job.service.SysJobLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Date;

/**
 * 抽象quartz调用
 *
 * @author wesley
 */
@Slf4j
@DisallowConcurrentExecution
public abstract class AbstractQuartzJob implements Job {
    /**
     * 线程本地变量
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    private LockTemplate lockTemplate;

    private final Date createTime = new Date();


    public AbstractQuartzJob() {
        lockTemplate = SpringUtil.getBean(LockTemplate.class);
        jbmRequestTemplate = SpringUtil.getBean(JbmRequestTemplate.class);
    }

    @Override
    public void execute(JobExecutionContext context) {
        final Date executeTime = new Date();
        final String jobKey = context.getJobDetail().getKey().toString();
        if (DateUtil.between(context.getTrigger().getStartTime(), context.getTrigger().getPreviousFireTime(), DateUnit.SECOND) <= 1) {
            return;
        }
        //获取锁
//        String key = jobKey + "_" + DateUtil.format(createTime, DatePattern.NORM_DATETIME_FORMAT);
        String key = jobKey;
//        Lock redisLock = redisLockRegistry.obtain(key);
        //建立一个十分钟的锁
        final LockInfo redisLock = lockTemplate.lock(key, 1000 * 60, 1000);

        SysJob sysJob = null;

        String data = null;
        try {
            //锁定5秒
            if (ObjectUtil.isEmpty(redisLock)) {
                log.info("正在处理中，请勿重复提交");
                return;
            }
            Object obj = context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES);
            if (ObjectUtil.isEmpty(obj)) {
                throw new RuntimeException("任务信息存在错误");
            }
            if (obj instanceof SysJob) {
                sysJob = (SysJob) obj;
            } else if (obj instanceof String) {
                data = obj.toString();
            } else {
                data = JSON.toJSONString(obj);
            }
            if (StrUtil.isNotBlank(data)) {
                sysJob = JSON.parseObject(data, SysJob.class);
            }
            if (ObjectUtil.isEmpty(sysJob)) {
                throw new RuntimeException("任务信息存在错误");
            }
            before(context, sysJob);
            doExecute(context, sysJob);
            after(context, sysJob, null);
        } catch (Exception e) {
            log.error("任务执行异常:{}", data, e);
            after(context, sysJob, e);
        } finally {
            final Date endTime = new DateTime();
            final Long runTime = endTime.getTime() - executeTime.getTime();
            ThreadUtil.safeSleep(runTime > 1000 ? 0 : 1000);
            lockTemplate.releaseLock(redisLock);
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     * @param sysJob  系统计划任务
     */
    protected void before(JobExecutionContext context, SysJob sysJob) {
        threadLocal.set(new Date());
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     * @param sysJob  系统计划任务
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e) {
        if (BooleanUtil.isFalse(sysJob.getRecordLog())) {
            return;
        }
        Date startTime = threadLocal.get();
        threadLocal.remove();
        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setStopTime(DateTime.now());
        long runMs = DateUtil.between(sysJobLog.getStopTime(), sysJobLog.getStartTime(), DateUnit.MS);
        sysJobLog.setRunTime(runMs);
        sysJobLog.setJobMessage(sysJobLog.getJobName() + "总共耗时：" + runMs + "毫秒");
        if (e != null) {
            sysJobLog.setStatus("1");
            String errorMsg = StrUtil.sub(ExceptionUtil.getMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        } else {
            sysJobLog.setStatus("0");
        }
        // 写入数据库当中
        SpringUtil.getBean(SysJobLogService.class).saveEntity(sysJobLog);
    }


    private final JbmRequestTemplate jbmRequestTemplate;

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param sysJob  系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception {
        jbmRequestTemplate.request(sysJob.getInvokeTarget(), sysJob.getMethodType(), null);
//        if (JobInvokeUtil.isFeign(sysJob.getInvokeTarget())) {
//            String url = JobInvokeUtil.feignToUrl(sysJob.getInvokeTarget());
//            if (StrUtil.isEmpty(url)) {
//                throw new RuntimeException("远程服务没有启动");
//            }
//            HttpRequest httpRequest = HttpRequest.get(url).timeout(3000);
//            if (HttpMethod.POST.matches(sysJob.getMethodType())) {
//                httpRequest = HttpRequest.post(url).timeout(3000);
//            }
//            ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
//            httpRequest.header(JbmSecurityConstants.AUTHORIZATION_HEADER, SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken);
//            HttpResponse httpResponse = httpRequest.execute();
//            log.info("执行URL状态为[{}],结果[{}]", httpResponse.getStatus(), httpResponse.body());
//        }
//        //如果和HTTP链接
//        else if (HttpUtil.isHttp(sysJob.getInvokeTarget()) || HttpUtil.isHttps(sysJob.getInvokeTarget())) {
//            HttpRequest httpRequest = HttpRequest.get(sysJob.getInvokeTarget()).timeout(3000);
//            if (HttpMethod.POST.matches(sysJob.getMethodType())) {
//                httpRequest = HttpRequest.post(sysJob.getInvokeTarget()).timeout(3000);
//            }
//            ClientTokenModel clientTokenModel = saOAuth2Template.generateClientToken(SpringUtil.getApplicationName(), "*");
//            httpRequest.header(JbmSecurityConstants.AUTHORIZATION_HEADER, SaManager.getConfig().getTokenPrefix() + " " + clientTokenModel.clientToken);
//            HttpResponse httpResponse = httpRequest.execute();
//            log.info("执行URL状态为[{}],结果[{}]", httpResponse.getStatus(), httpResponse.body());
//        } else {
//            JobInvokeUtil.invokeMethod(sysJob);
//        }
    }
}

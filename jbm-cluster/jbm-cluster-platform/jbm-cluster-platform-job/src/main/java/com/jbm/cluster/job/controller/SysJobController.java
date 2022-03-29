package com.jbm.cluster.job.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.jbm.cluster.api.model.entitys.job.SysJob;
import com.jbm.cluster.common.auth.RequiresPermissions;
import com.jbm.cluster.common.constants.CommonConstants;
import com.jbm.cluster.common.exception.job.TaskException;
import com.jbm.cluster.common.security.JbmClusterHelper;
import com.jbm.cluster.job.service.SysJobService;
import com.jbm.cluster.job.util.CronUtils;
import com.jbm.cluster.job.util.ScheduleUtils;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 调度任务信息操作处理
 *
 * @author wesley
 */
@Slf4j
@Api(tags = "调度任务接口")
@RestController
@RequestMapping("/sysJob")
public class SysJobController extends MasterDataCollection<SysJob, SysJobService> {

    /**
     * 导出定时任务列表
     */
    @RequiresPermissions("monitor:job:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJob sysJob) {
        List<SysJob> list = this.service.selectJobList(sysJob);
        ExcelWriter excelWriter = ExcelUtil.getBigWriter().write(list, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            excelWriter.flush(response.getOutputStream());
        } catch (Exception e) {
            log.error("导出Excel异常{}", e.getMessage());
        }
    }


    /**
     * 新增定时任务
     */
    @RequiresPermissions("monitor:job:add")
    @PostMapping
    public ResultBody add(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), CommonConstants.LOOKUP_RMI)) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失 败，目标字符串不允许'rmi'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{CommonConstants.LOOKUP_LDAP, CommonConstants.LOOKUP_LDAPS})) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{CommonConstants.HTTP, CommonConstants.HTTPS})) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), CommonConstants.JOB_ERROR_STR)) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(JbmClusterHelper.getUser().getUsername());
        return ResultBody.success(this.service.insertEntity(job), "");
    }

    /**
     * 修改定时任务
     */
    @RequiresPermissions("monitor:job:edit")
    @PutMapping
    public ResultBody edit(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), CommonConstants.LOOKUP_RMI)) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{CommonConstants.LOOKUP_LDAP, CommonConstants.LOOKUP_LDAPS})) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{CommonConstants.HTTP, CommonConstants.HTTPS})) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), CommonConstants.JOB_ERROR_STR)) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(JbmClusterHelper.getUser().getUsername());
        return ResultBody.ok().data(this.service.updateById(job));
    }

    /**
     * 定时任务状态修改
     */
    @RequiresPermissions("monitor:job:changeStatus")
    @PutMapping("/changeStatus")
    public ResultBody changeStatus(@RequestBody SysJob job) throws SchedulerException {
        SysJob newJob = this.service.selectById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return ResultBody.ok().data(this.service.changeStatus(newJob));
    }

    /**
     * 定时任务立即执行一次
     */
    @RequiresPermissions("monitor:job:changeStatus")
    @PutMapping("/run")
    public ResultBody run(@RequestBody SysJob job) throws SchedulerException {
        this.service.run(job);
        return ResultBody.ok();
    }

}

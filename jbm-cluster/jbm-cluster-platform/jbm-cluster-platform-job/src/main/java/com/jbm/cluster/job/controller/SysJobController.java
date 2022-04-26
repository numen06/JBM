package com.jbm.cluster.job.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.common.core.annotation.JbmClusterEvent;
import com.jbm.cluster.common.core.annotation.JbmClusterScheduled;
import com.jbm.cluster.common.security.annotation.RequiresPermissions;
import com.jbm.cluster.common.security.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.cluster.job.service.SysJobService;
import com.jbm.cluster.job.util.CronUtils;
import com.jbm.cluster.job.util.ScheduleUtils;
import com.jbm.framework.exceptions.job.TaskException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
    @ApiOperation(value = "导出定时任务列表", notes = "")
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
    @JbmClusterEvent(eventTypeClass = SysJob.class)
    @JbmClusterScheduled(cron = "*/5 * * * * ?")
    @ApiOperation(value = "新增定时任务", notes = "")
    @RequiresPermissions("monitor:job:add")
    @PostMapping("/add")
    public ResultBody add(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), JbmConstants.LOOKUP_RMI)) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.LOOKUP_LDAP, JbmConstants.LOOKUP_LDAPS})) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.HTTP, JbmConstants.HTTPS})) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), JbmConstants.JOB_ERROR_STR)) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(SecurityUtils.getUsername());
//        job.setCreateBy(JbmClusterHelper.getUser().getUsername());
        return ResultBody.success(this.service.insertJob(job), "");
    }

    /**
     * 修改定时任务
     */
    @ApiOperation(value = "修改定时任务", notes = "")
    @RequiresPermissions("monitor:job:edit")
    @PostMapping("/edit")
    public ResultBody edit(@RequestBody SysJob job) throws SchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        } else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), JbmConstants.LOOKUP_RMI)) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.LOOKUP_LDAP, JbmConstants.LOOKUP_LDAPS})) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.HTTP, JbmConstants.HTTPS})) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), JbmConstants.JOB_ERROR_STR)) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateBy(SecurityUtils.getUsername());
//        job.setCreateBy(JbmClusterHelper.getUser().getUsername());
        return ResultBody.ok().data(this.service.updateById(job));
    }

    /**
     * 定时任务状态修改
     */
    @ApiOperation(value = "定时任务状态修改", notes = "")
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
    @ApiOperation(value = "定时任务立即执行一次", notes = "")
    @RequiresPermissions("monitor:job:changeStatus")
    @PutMapping("/run")
    public ResultBody run(@RequestBody SysJob job) throws SchedulerException {
        return ResultBody.ok().data(this.service.run(job));
    }

}

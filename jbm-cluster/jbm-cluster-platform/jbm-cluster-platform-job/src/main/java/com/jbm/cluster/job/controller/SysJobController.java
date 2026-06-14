package com.jbm.cluster.job.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.api.event.annotation.BusinessEventListener;
import com.jbm.cluster.api.job.SchedulerJob;
import com.jbm.cluster.common.basic.annotation.JbmClusterEvent;
import com.jbm.cluster.common.basic.annotation.JbmClusterScheduled;
import com.jbm.cluster.common.security.annotation.Logical;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.common.security.annotation.RequiresPermissions;
import com.jbm.cluster.job.exception.JobSchedulerException;
import com.jbm.cluster.job.service.SysJobService;
import com.jbm.cluster.job.util.CronUtils;
import com.jbm.framework.exceptions.job.TaskException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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


    @SchedulerJob(name = "测试定时接受任务2", cron = "0/5 * * * * ?", enable = false)
    @ApiOperation(value = "接受测试信息", notes = "")
    @GetMapping("/test")
    public ResultBody test() {
        return ResultBody.ok();
    }

    /**
     * 导出定时任务列表
     */
    @ApiOperation(value = "导出定时任务列表", notes = "")
    @RequiresPermissions(value = {"monitor:job:export", "job_export"}, logical = Logical.OR)
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
    @RequiresPermissions(value = {"monitor:job:add", "job_add"}, logical = Logical.OR)
    @PostMapping("/add")
    public ResultBody add(@RequestBody SysJob job) throws JobSchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        }
//        else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), JbmConstants.LOOKUP_RMI)) {
//            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.LOOKUP_LDAP, JbmConstants.LOOKUP_LDAPS})) {
//            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.HTTP, JbmConstants.HTTPS})) {
//            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), JbmConstants.JOB_ERROR_STR)) {
//            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
//        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
//            return ResultBody.failed().msg("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
//        }
        job.setCreateBy(SecurityUtils.getUsername());
//        job.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        return ResultBody.success(this.service.insertJob(job), "");
    }

    /**
     * 修改定时任务
     */
    @ApiOperation(value = "修改定时任务", notes = "")
    @RequiresPermissions(value = {"monitor:job:edit", "job_edit"}, logical = Logical.OR)
    @PostMapping("/edit")
    public ResultBody edit(@RequestBody SysJob job) throws JobSchedulerException, TaskException {
        if (!CronUtils.isValid(job.getCronExpression())) {
            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        }
//        else if (StrUtil.containsIgnoreCase(job.getInvokeTarget(), JbmConstants.LOOKUP_RMI)) {
//            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.LOOKUP_LDAP, JbmConstants.LOOKUP_LDAPS})) {
//            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), new String[]{JbmConstants.HTTP, JbmConstants.HTTPS})) {
//            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
//        } else if (StrUtil.containsAnyIgnoreCase(job.getInvokeTarget(), JbmConstants.JOB_ERROR_STR)) {
//            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
//        } else if (!ScheduleUtils.whiteList(job.getInvokeTarget())) {
//            return ResultBody.failed().msg("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
//        }
        job.setCreateBy(SecurityUtils.getUsername());
//        job.setCreateBy(SecurityUtils.getLoginUser().getUsername());
        return ResultBody.ok().data(this.service.updateById(job));
    }

    /**
     * 定时任务状态修改
     */
    @ApiOperation(value = "定时任务状态修改", notes = "")
    @RequiresPermissions(value = {"monitor:job:changeStatus", "job_status"}, logical = Logical.OR)
    @PutMapping("/changeStatus")
    public ResultBody changeStatus(@RequestBody SysJob job) {
        SysJob newJob = this.service.selectById(job.getJobId());
        newJob.setStatus(job.getStatus());
        return ResultBody.callback(() -> {
            try {
                return this.service.changeStatus(newJob);
            } catch (JobSchedulerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 定时任务立即执行一次
     */
    @ApiOperation(value = "定时任务立即执行一次", notes = "")
    @RequiresPermissions(value = {"monitor:job:changeStatus", "job_run"}, logical = Logical.OR)
    @PutMapping("/run")
    public ResultBody run(@RequestBody SysJob job) {
        return ResultBody.callback(() -> {
            try {
                return this.service.run(job);
            } catch (JobSchedulerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ApiOperation(value = "定时任务详情", notes = "")
    @GetMapping("/{jobId}")
    public ResultBody<SysJob> get(@PathVariable Long jobId) {
        return ResultBody.callback(() -> this.service.selectById(jobId));
    }

    @ApiOperation(value = "创建定时任务", notes = "REST 兼容入口")
    @RequiresPermissions(value = {"monitor:job:add", "job_add"}, logical = Logical.OR)
    @PostMapping
    public ResultBody create(@RequestBody SysJob job) throws JobSchedulerException, TaskException {
        return add(job);
    }

    @ApiOperation(value = "更新定时任务", notes = "REST 兼容入口")
    @RequiresPermissions(value = {"monitor:job:edit", "job_edit"}, logical = Logical.OR)
    @PutMapping("/{jobId}")
    public ResultBody update(@PathVariable Long jobId, @RequestBody SysJob job) throws JobSchedulerException, TaskException {
        job.setJobId(jobId);
        return edit(job);
    }

    @ApiOperation(value = "删除定时任务", notes = "")
    @RequiresPermissions(value = {"monitor:job:remove", "job_delete"}, logical = Logical.OR)
    @DeleteMapping("/{jobId}")
    public ResultBody<Integer> delete(@PathVariable Long jobId) {
        SysJob job = new SysJob();
        job.setJobId(jobId);
        return ResultBody.callback(() -> {
            try {
                return this.service.deleteJob(job);
            } catch (JobSchedulerException e) {
                throw new RuntimeException(e);
            }
        });
    }

}

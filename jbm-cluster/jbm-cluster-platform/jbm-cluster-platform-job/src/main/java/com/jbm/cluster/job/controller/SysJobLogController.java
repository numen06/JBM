package com.jbm.cluster.job.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.jbm.cluster.api.entitys.job.SysJobLog;
import com.jbm.cluster.common.security.annotation.RequiresPermissions;
import com.jbm.cluster.job.service.SysJobLogService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 调度日志操作处理
 *
 * @author wesley
 */
@Slf4j
@Api(tags = "调度任务日志接口")
@RestController
@RequestMapping("/sysJobLog")
public class SysJobLogController extends MasterDataCollection<SysJobLog, SysJobLogService> {


    /**
     * 导出定时任务调度日志列表
     */
    @ApiOperation(value = "导出定时任务调度日志列表")
    @RequiresPermissions("monitor:job:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysJobLog sysJobLog) {
        List<SysJobLog> list = this.service.selectJobLogList(sysJobLog);
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
     * 清空定时任务调度日志
     */
    @ApiOperation(value = "清空定时任务调度日志")
    @RequiresPermissions("monitor:job:remove")
    @DeleteMapping("/clean")
    public ResultBody clean() {
        this.service.cleanJobLog();
        return ResultBody.ok();
    }
}

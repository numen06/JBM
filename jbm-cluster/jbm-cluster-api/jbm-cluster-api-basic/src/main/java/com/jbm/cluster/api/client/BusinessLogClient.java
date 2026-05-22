package com.jbm.cluster.api.client;

import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 业务日志 Feign 客户端（MDC 精简版）
 * 仅保留 MDC 采集 + 查询所需的最小接口集合，方便将业务链路日志写入中央日志平台。
 *
 * 推荐搭配 MDC（Mapped Diagnostic Context）使用：
 * - 创建日志时写入 traceId / businessId 等上下文字段
 * - 通过 append 接口实时补充业务步骤
 * - 通过查询接口在排查链路问题时一次性拿到完整日志
 * 
 * @author wesley
 */
@FeignClient(
    name = "jbm-cluster-platform-logs",
    path = "/businessLog",
    contextId = "businessLogClient"
)
public interface BusinessLogClient {

    /**
     * 创建业务日志
     * 
     * 使用 CreateBusinessLogForm，支持两种格式：
     * 1. 原有格式：设置 module、operation、userId 等
     * 2. 集成格式：设置 businessType、businessId、source 等
     * 
     * @param form 创建业务日志表单
     * @return 返回包含 logId 的 Map
     */
    @PostMapping("/create")
    Map<String, String> createLog(@RequestBody CreateBusinessLogForm form);

    /**
     * 追加日志内容（表单方式，推荐）
     *
     * @param form 追加日志表单
     * @return 是否成功
     */
    @PostMapping("/append")
    Boolean appendLog(@RequestBody AppendBusinessLogForm form);

    /**
     * 追加日志内容（简化方式）
     * 仅传递 logId 和 content
     */
    @PostMapping("/append/{logId}")
    Boolean appendLogSimple(@PathVariable("logId") String logId,
                            @RequestBody String content);
    
    /**
     * 查询业务日志（完整内容）
     * 
     * @param logId 日志ID
     * @param formatted 是否格式化（true: 添加头部信息和行号，false: 仅返回原始日志内容）
     * @return 完整日志内容
     */
    @GetMapping("/getFullContent/{logId}")
    String getLogFullContent(@PathVariable("logId") String logId,
                             @RequestParam(value = "formatted", required = false, defaultValue = "false") Boolean formatted);

    /**
     * 分页查询业务日志
     * 
     * @param form 查询表单
     * @return 分页结果
     */
    @PostMapping("/query")
    DataPaging<BusinessLog> queryLogs(@RequestBody BusinessLogForm form);
}


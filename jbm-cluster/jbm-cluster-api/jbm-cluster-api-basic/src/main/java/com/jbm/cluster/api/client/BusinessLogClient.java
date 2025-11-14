package com.jbm.cluster.api.client;

import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务日志Feign客户端
 * 提供给其他服务调用的业务日志接口
 * 
 * 使用方式：
 * 1. 在需要调用的服务中引入 jbm-cluster-api-basic 模块
 * 2. 注入此客户端：@Autowired private BusinessLogClient businessLogClient;
 * 3. 调用相应的方法即可
 * 
 * 注意：
 * - 所有接口共用 BusinessLogController 中的原有接口
 * - 使用共享的 Form/Entity（在 jbm-cluster-api-basic 模块中）
 * - CreateBusinessLogForm 支持两种使用方式：原有格式或集成格式
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
    ResultBody<Map<String, String>> createLog(@RequestBody CreateBusinessLogForm form);

    /**
     * 追加日志内容（完整表单）
     * 
     * @param form 追加日志表单
     * @return 是否成功
     */
    @PostMapping("/append")
    ResultBody<Boolean> appendLog(@RequestBody AppendBusinessLogForm form);
    
    /**
     * 追加日志内容（简化版）
     * 仅传递 logId 和 content
     * 
     * @param logId 日志ID
     * @param content 追加的内容
     * @return 是否成功
     */
    @PostMapping("/append/{logId}")
    ResultBody<Boolean> appendLogSimple(@PathVariable("logId") String logId, 
                                       @RequestBody String content);
    
    /**
     * 查询业务日志（多行格式）
     * 
     * @param logId 日志ID
     * @return 日志记录列表
     */
    @GetMapping("/getMultiLine/{logId}")
    ResultBody<List<BusinessLog>> getLogMultiLine(@PathVariable("logId") String logId);

    /**
     * 查询业务日志（完整内容）
     * 
     * @param logId 日志ID
     * @param formatted 是否格式化（true: 添加头部信息和行号，false: 仅返回原始日志内容）
     * @return 完整日志内容
     */
    @GetMapping("/getFullContent/{logId}")
    ResultBody<String> getLogFullContent(@PathVariable("logId") String logId,
                                        @RequestParam(value = "formatted", required = false, defaultValue = "false") Boolean formatted);

    /**
     * 删除业务日志
     * 
     * @param logId 日志ID
     * @return 是否成功
     */
    @DeleteMapping("/delete/{logId}")
    ResultBody<Boolean> deleteLog(@PathVariable("logId") String logId);

    /**
     * 更新日志过期时间
     * 
     * @param logId 日志ID
     * @param expireDays 过期天数
     * @return 是否成功
     */
    @PutMapping("/updateExpireTime/{logId}/{expireDays}")
    ResultBody<Boolean> updateExpireTime(@PathVariable("logId") String logId,
                                        @PathVariable("expireDays") Integer expireDays);

    /**
     * 生成日志临时访问URL
     * 
     * @param logId 日志ID
     * @param expireMinutes 过期时间（分钟）
     * @param baseUrl 基础URL（可选）
     * @return URL信息（包含 url, expires, signature 等）
     */
    @GetMapping("/generateUrl/{logId}")
    ResultBody<Map<String, String>> generateTemporaryUrl(@PathVariable("logId") String logId,
                                                         @RequestParam(value = "expireMinutes", required = false, defaultValue = "60") Integer expireMinutes,
                                                         @RequestParam(value = "baseUrl", required = false) String baseUrl);

    /**
     * 获取日志总行数
     * 
     * @param logId 日志ID
     * @return 总行数
     */
    @GetMapping("/getTotalLines/{logId}")
    ResultBody<Integer> getLogTotalLines(@PathVariable("logId") String logId);
    
    /**
     * 按行号范围查询日志
     * 
     * @param logId 日志ID
     * @param startLine 起始行号
     * @param endLine 结束行号
     * @return 日志记录列表
     */
    @GetMapping("/getByLineRange/{logId}")
    ResultBody<List<BusinessLog>> getLogByLineRange(@PathVariable("logId") String logId,
                                                    @RequestParam(value = "startLine", required = false, defaultValue = "1") Integer startLine,
                                                    @RequestParam(value = "endLine", required = false, defaultValue = "-1") Integer endLine);
    
    /**
     * 分页查询业务日志
     * 
     * @param form 查询表单
     * @return 分页结果
     */
    @PostMapping("/query")
    ResultBody<DataPaging<BusinessLog>> queryLogs(@RequestBody BusinessLogForm form);
}


package com.jbm.cluster.api.client;

import com.jbm.cluster.api.model.log.BusinessLogRequest;
import com.jbm.cluster.api.model.log.BusinessLogResponse;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 业务日志Feign客户端
 * 提供给其他服务调用的业务日志接口
 * 
 * 使用方式：
 * 1. 在需要调用的服务中引入此API模块
 * 2. 注入此客户端：@Autowired private BusinessLogClient businessLogClient;
 * 3. 调用相应的方法即可
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
     * @param request 业务日志请求
     * @return 返回logId
     */
    @PostMapping("/api/create")
    ResultBody<Map<String, String>> createLog(@RequestBody BusinessLogRequest request);

    /**
     * 追加日志内容
     * 
     * @param logId 日志ID
     * @param content 日志内容
     * @return 是否成功
     */
    @PostMapping("/api/append/{logId}")
    ResultBody<Boolean> appendLog(@PathVariable("logId") String logId, 
                                  @RequestBody String content);

    /**
     * 通过业务类型和业务ID追加日志
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 日志内容
     * @return 是否成功
     */
    @PostMapping("/api/append/byBusinessId")
    ResultBody<Boolean> appendLogByBusinessId(@RequestParam("businessType") String businessType,
                                              @RequestParam("businessId") String businessId,
                                              @RequestBody String content);

    /**
     * 查询业务日志（多行格式）
     * 
     * @param logId 日志ID
     * @return 日志记录列表
     */
    @GetMapping("/api/get/{logId}")
    ResultBody<BusinessLogResponse> getLog(@PathVariable("logId") String logId);

    /**
     * 查询业务日志（完整内容）
     * 
     * @param logId 日志ID
     * @param formatted 是否格式化
     * @return 完整日志内容
     */
    @GetMapping("/api/getFullContent/{logId}")
    ResultBody<String> getLogFullContent(@PathVariable("logId") String logId,
                                        @RequestParam(value = "formatted", required = false, defaultValue = "false") Boolean formatted);

    /**
     * 通过业务类型和业务ID查询日志
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 日志记录
     */
    @GetMapping("/api/getByBusinessId")
    ResultBody<BusinessLogResponse> getLogByBusinessId(@RequestParam("businessType") String businessType,
                                                       @RequestParam("businessId") String businessId);

    /**
     * 删除业务日志
     * 
     * @param logId 日志ID
     * @return 是否成功
     */
    @DeleteMapping("/api/delete/{logId}")
    ResultBody<Boolean> deleteLog(@PathVariable("logId") String logId);

    /**
     * 更新日志过期时间
     * 
     * @param logId 日志ID
     * @param expireDays 过期天数
     * @return 是否成功
     */
    @PutMapping("/api/updateExpireTime/{logId}/{expireDays}")
    ResultBody<Boolean> updateExpireTime(@PathVariable("logId") String logId,
                                        @PathVariable("expireDays") Integer expireDays);

    /**
     * 生成日志临时访问URL
     * 
     * @param logId 日志ID
     * @param expireMinutes 过期时间（分钟）
     * @param baseUrl 基础URL（可选）
     * @return URL信息
     */
    @GetMapping("/api/generateUrl/{logId}")
    ResultBody<Map<String, String>> generateTemporaryUrl(@PathVariable("logId") String logId,
                                                         @RequestParam(value = "expireMinutes", required = false, defaultValue = "60") Integer expireMinutes,
                                                         @RequestParam(value = "baseUrl", required = false) String baseUrl);

    /**
     * 获取日志总行数
     * 
     * @param logId 日志ID
     * @return 总行数
     */
    @GetMapping("/api/getTotalLines/{logId}")
    ResultBody<Integer> getLogTotalLines(@PathVariable("logId") String logId);

    /**
     * 按行号范围查询日志
     * 
     * @param logId 日志ID
     * @param startLine 起始行号
     * @param endLine 结束行号
     * @return 日志内容
     */
    @GetMapping("/api/getByLineRange/{logId}")
    ResultBody<String> getLogByLineRange(@PathVariable("logId") String logId,
                                        @RequestParam(value = "startLine", required = false, defaultValue = "1") Integer startLine,
                                        @RequestParam(value = "endLine", required = false, defaultValue = "-1") Integer endLine);
}


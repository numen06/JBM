package com.jbm.cluster.common.basic.module;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.client.BusinessLogClient;
import com.jbm.cluster.api.model.log.BusinessLogEvent;
import com.jbm.cluster.api.model.log.BusinessLogEventType;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.Map;

/**
 * 业务日志模板类
 * 为其他项目提供便捷的业务日志操作接口
 * 支持同步和异步两种方式创建日志
 * 
 * 使用方式：
 * 1. 异步方式（RabbitMQ）- 高性能，不阻塞业务，但无法立即获取 logId
 * 2. 同步方式（Feign）- 可以立即获取 logId，但会阻塞业务流程
 * 
 * @author wesley
 */
@Slf4j
public class JbmBusinessLogTemplate {

    @Autowired(required = false)
    private StreamBridge streamBridge;
    
    @Autowired(required = false)
    private BusinessLogClient businessLogClient;

    // ==================== 同步方式（立即返回 logId）====================
    
    /**
     * 同步创建业务日志并立即返回 logId
     * 
     * ⚠️ 注意：此方法通过 Feign 同步调用日志服务，会阻塞当前线程
     * 适用于需要立即获取 logId 的场景
     * 
     * @param businessType 业务类型（如：订单、支付、用户等）
     * @param businessId 业务ID（如：订单号、支付流水号等）
     * @param content 日志内容
     * @param expireDays 过期天数（7、30、90、180、365）
     * @param source 日志来源（如：订单服务、支付服务等）
     * @return logId - 业务日志ID
     * @throws RuntimeException 如果创建失败或 BusinessLogClient 未注入
     */
    public String createLogSync(String businessType, String businessId, String content, Integer expireDays, String source) {
        if (businessLogClient == null) {
            throw new RuntimeException("BusinessLogClient 未注入，无法使用同步创建功能。请确保已配置 Feign 客户端。");
        }
        
        try {
            // 构建 CreateBusinessLogForm（使用集成格式字段）
            com.jbm.cluster.api.form.log.CreateBusinessLogForm form = new com.jbm.cluster.api.form.log.CreateBusinessLogForm();
            form.setBusinessType(businessType);
            form.setBusinessId(businessId);
            form.setContent(content);
            form.setExpireDays(expireDays != null ? expireDays : 30);
            form.setSource(source);
            
            // 调用 Feign 客户端创建日志
            ResultBody<Map<String, String>> result = businessLogClient.createLog(form);
            
            if (result.getSuccess() && result.getResult() != null) {
                String logId = result.getResult().get("logId");
                log.debug("同步创建业务日志成功: logId={}, businessType={}, businessId={}", 
                         logId, businessType, businessId);
                return logId;
            } else {
                throw new RuntimeException("创建业务日志失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("同步创建业务日志失败: businessType={}, businessId={}", businessType, businessId, e);
            throw new RuntimeException("同步创建业务日志失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步创建业务日志（使用默认过期时间30天）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 日志内容
     * @param source 日志来源
     * @return logId - 业务日志ID
     */
    public String createLogSync(String businessType, String businessId, String content, String source) {
        return createLogSync(businessType, businessId, content, 30, source);
    }
    
    // ==================== 异步方式（高性能，不返回 logId）====================
    
    /**
     * 异步创建业务日志（推荐用于高性能场景）
     * 
     * ⚠️ 注意：此方法通过 RabbitMQ 异步发送，不会返回 logId
     * 建议使用 businessType + businessId 作为唯一标识
     * 
     * @param businessType 业务类型（如：订单、支付、用户等）
     * @param businessId 业务ID（如：订单号、支付流水号等）
     * @param content 日志内容
     * @param expireDays 过期天数（7、30、90、180、365）
     * @param source 日志来源（如：订单服务、支付服务等）
     */
    public void createLogAsync(String businessType, String businessId, String content, Integer expireDays, String source) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.CREATE);
        event.setBusinessType(businessType);
        event.setBusinessId(businessId);
        event.setContent(content);
        event.setExpireDays(expireDays);
        event.setSource(source);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 异步创建业务日志（使用默认过期时间30天）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 日志内容
     * @param source 日志来源
     */
    public void createLogAsync(String businessType, String businessId, String content, String source) {
        createLogAsync(businessType, businessId, content, 30, source);
    }
    
    /**
     * 创建业务日志（默认使用异步方式）
     * 
     * 为了保持向后兼容和推荐最佳实践，此方法默认使用异步方式
     * 如需同步获取 logId，请使用 createLogSync() 方法
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 日志内容
     * @param expireDays 过期天数
     * @param source 日志来源
     */
    public void createLog(String businessType, String businessId, String content, Integer expireDays, String source) {
        createLogAsync(businessType, businessId, content, expireDays, source);
    }

    /**
     * 创建业务日志（默认使用异步方式，使用默认过期时间30天）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 日志内容
     * @param source 日志来源
     */
    public void createLog(String businessType, String businessId, String content, String source) {
        createLogAsync(businessType, businessId, content, 30, source);
    }

    /**
     * 追加日志内容
     * 
     * @param logId 业务日志ID
     * @param content 追加的日志内容
     */
    public void appendLog(String logId, String content) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.APPEND);
        event.setLogId(logId);
        event.setContent(content);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 通过业务类型和业务ID追加日志
     * 当无法获取 logId 时，可以使用此方法
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param content 追加的日志内容
     */
    public void appendLogByBusinessId(String businessType, String businessId, String content) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.APPEND);
        event.setBusinessType(businessType);
        event.setBusinessId(businessId);
        event.setContent(content);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 删除业务日志（标记为删除）
     * 
     * @param logId 业务日志ID
     */
    public void deleteLog(String logId) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.DELETE);
        event.setLogId(logId);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 通过业务类型和业务ID删除日志
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    public void deleteLogByBusinessId(String businessType, String businessId) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.DELETE);
        event.setBusinessType(businessType);
        event.setBusinessId(businessId);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 更新日志过期时间
     * 
     * @param logId 业务日志ID
     * @param expireDays 新的过期天数
     */
    public void updateExpireTime(String logId, Integer expireDays) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.UPDATE_EXPIRE);
        event.setLogId(logId);
        event.setExpireDays(expireDays);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }

    /**
     * 生成日志访问URL（异步，用于外部访问）
     * 注意：此方法发送请求后需要通过回调或轮询获取结果
     * 
     * @param logId 业务日志ID
     * @param expireMinutes 过期时间（分钟）
     */
    public void generateLogUrl(String logId, Integer expireMinutes) {
        BusinessLogEvent event = new BusinessLogEvent();
        event.setEventType(BusinessLogEventType.GENERATE_URL);
        event.setLogId(logId);
        event.setExpireMinutes(expireMinutes);
        event.setTimestamp(System.currentTimeMillis());
        
        sendBusinessLogEvent(event);
    }
    
    /**
     * 生成日志访问URL（同步，立即返回URL）
     * 
     * @param logId 业务日志ID
     * @param expireMinutes 过期时间（分钟）
     * @return 临时访问URL
     * @throws RuntimeException 如果生成失败或 BusinessLogClient 未注入
     */
    public String generateLogUrlSync(String logId, Integer expireMinutes) {
        if (businessLogClient == null) {
            throw new RuntimeException("BusinessLogClient 未注入，无法使用同步生成URL功能。请确保已配置 Feign 客户端。");
        }
        
        try {
            ResultBody<Map<String, String>> result = 
                businessLogClient.generateTemporaryUrl(logId, expireMinutes, null);
            
            if (result.getSuccess() && result.getResult() != null) {
                String url = result.getResult().get("url");
                log.debug("同步生成日志访问URL成功: logId={}, url={}", logId, url);
                return url;
            } else {
                throw new RuntimeException("生成日志访问URL失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("同步生成日志访问URL失败: logId={}", logId, e);
            throw new RuntimeException("同步生成日志访问URL失败: " + e.getMessage(), e);
        }
    }

    // ==================== 便捷方法（异步版本）====================
    
    /**
     * 记录完整的业务操作日志（便捷方法，异步）
     * 自动格式化日志内容，包含操作人、操作时间等信息
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param operator 操作人
     * @param operation 操作内容
     * @param result 操作结果
     * @param source 日志来源
     */
    public void recordOperation(String businessType, String businessId, String operator, 
                                String operation, String result, String source) {
        String content = StrUtil.format("[{}] {} - 操作: {} - 结果: {}", 
                                       operator, 
                                       cn.hutool.core.date.DateUtil.now(), 
                                       operation, 
                                       result);
        createLogAsync(businessType, businessId, content, source);
    }
    
    /**
     * 记录完整的业务操作日志（便捷方法，同步返回 logId）
     * 自动格式化日志内容，包含操作人、操作时间等信息
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param operator 操作人
     * @param operation 操作内容
     * @param result 操作结果
     * @param source 日志来源
     * @return logId - 业务日志ID
     */
    public String recordOperationSync(String businessType, String businessId, String operator, 
                                     String operation, String result, String source) {
        String content = StrUtil.format("[{}] {} - 操作: {} - 结果: {}", 
                                       operator, 
                                       cn.hutool.core.date.DateUtil.now(), 
                                       operation, 
                                       result);
        return createLogSync(businessType, businessId, content, source);
    }

    /**
     * 记录错误日志（便捷方法，异步）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param errorMessage 错误消息
     * @param exception 异常对象（可选）
     * @param source 日志来源
     */
    public void recordError(String businessType, String businessId, String errorMessage, 
                           Throwable exception, String source) {
        String content = buildErrorContent(errorMessage, exception);
        createLogAsync(businessType, businessId, content, 90, source);
    }
    
    /**
     * 记录错误日志（便捷方法，同步返回 logId）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param errorMessage 错误消息
     * @param exception 异常对象（可选）
     * @param source 日志来源
     * @return logId - 业务日志ID
     */
    public String recordErrorSync(String businessType, String businessId, String errorMessage, 
                                 Throwable exception, String source) {
        String content = buildErrorContent(errorMessage, exception);
        return createLogSync(businessType, businessId, content, 90, source);
    }
    
    /**
     * 构建错误日志内容
     */
    private String buildErrorContent(String errorMessage, Throwable exception) {
        StringBuilder content = new StringBuilder();
        content.append(StrUtil.format("[ERROR] {} - {}", cn.hutool.core.date.DateUtil.now(), errorMessage));
        
        if (exception != null) {
            content.append("\n异常信息: ").append(exception.getClass().getName());
            content.append("\n异常消息: ").append(exception.getMessage());
            // 添加堆栈信息（前10行）
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                content.append("\n堆栈跟踪:");
                int limit = Math.min(10, stackTrace.length);
                for (int i = 0; i < limit; i++) {
                    content.append("\n  at ").append(stackTrace[i].toString());
                }
            }
        }
        
        return content.toString();
    }

    /**
     * 记录步骤日志（便捷方法）
     * 适用于需要记录多个步骤的长流程业务
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param stepName 步骤名称
     * @param stepResult 步骤结果
     * @param details 详细信息
     * @param source 日志来源
     */
    public void recordStep(String businessType, String businessId, String stepName, 
                          String stepResult, String details, String source) {
        String content = StrUtil.format("[STEP] {} - {} - {}\n详情: {}", 
                                       cn.hutool.core.date.DateUtil.now(), 
                                       stepName, 
                                       stepResult, 
                                       details);
        // 使用 append 方式，避免创建多个日志记录
        appendLogByBusinessId(businessType, businessId, content);
    }

    /**
     * 批量记录日志（适用于批处理场景）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param logs 日志内容列表
     * @param source 日志来源
     */
    public void recordBatch(String businessType, String businessId, java.util.List<String> logs, String source) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        
        String content = String.join("\n", logs);
        createLog(businessType, businessId, content, source);
    }

    /**
     * 发送业务日志事件到消息队列
     * 
     * @param event 业务日志事件
     */
    private void sendBusinessLogEvent(BusinessLogEvent event) {
        try {
            if (streamBridge == null) {
                log.error("StreamBridge未注入，无法发送业务日志事件。请检查Spring Cloud Stream配置。");
                return;
            }
            
            final Message<BusinessLogEvent> message = MessageBuilder.withPayload(event).build();
            boolean sent = streamBridge.send(QueueConstants.BUSINESS_LOG_STREAM, message);
            
            if (!sent) {
                log.error("业务日志事件发送失败: {}", JSON.toJSONString(event));
            } else {
                log.debug("业务日志事件已发送: type={}, businessType={}, businessId={}, logId={}", 
                         event.getEventType(), event.getBusinessType(), event.getBusinessId(), event.getLogId());
            }
        } catch (Exception e) {
            log.error("发送业务日志事件异常: {}", e.getMessage(), e);
        }
    }

    // ==================== 实时追加场景（推荐用于导入、处理任务等）====================
    
    /**
     * 初始化实时日志（用于需要不断追加内容的场景）
     * 
     * 典型使用场景：
     * - 数据导入任务：导入开始创建日志，过程中不断追加进度
     * - 批处理任务：任务开始创建日志，每处理一批追加一次
     * - 长流程业务：流程开始创建日志，每个步骤追加记录
     * 
     * @param businessType 业务类型（如：DATA_IMPORT、BATCH_TASK）
     * @param businessId 业务ID（如：任务ID、批次号）
     * @param title 日志标题（如：导入用户数据、批量更新订单）
     * @param expireDays 过期天数
     * @param source 日志来源
     * @return logId - 用于后续追加内容
     * 
     * @example
     * <pre>
     * // 1. 初始化日志
     * String logId = businessLogTemplate.initRealtimeLog(
     *     "DATA_IMPORT", 
     *     "TASK-20250111-001", 
     *     "导入用户数据", 
     *     30, 
     *     "import-service"
     * );
     * 
     * // 2. 过程中不断追加
     * businessLogTemplate.appendContent(logId, "开始读取文件...");
     * businessLogTemplate.appendContent(logId, "已导入100条记录");
     * businessLogTemplate.appendContent(logId, "已导入200条记录");
     * businessLogTemplate.appendContent(logId, "导入完成，共导入500条记录");
     * </pre>
     */
    public String initRealtimeLog(String businessType, String businessId, String title, 
                                 Integer expireDays, String source) {
        String initialContent = StrUtil.format("[{}] {} - 任务开始\n" +
                                               "----------------------------------------\n" +
                                               "业务类型: {}\n" +
                                               "业务ID: {}\n" +
                                               "开始时间: {}\n" +
                                               "----------------------------------------\n",
                                               title,
                                               title,
                                               businessType,
                                               businessId,
                                               cn.hutool.core.date.DateUtil.now());
        
        return createLogSync(businessType, businessId, initialContent, expireDays, source);
    }
    
    /**
     * 初始化实时日志（使用默认过期时间30天）
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param title 日志标题
     * @param source 日志来源
     * @return logId
     */
    public String initRealtimeLog(String businessType, String businessId, String title, String source) {
        return initRealtimeLog(businessType, businessId, title, 30, source);
    }
    
    /**
     * 追加内容到实时日志（异步，高性能）
     * 
     * ⚠️ 推荐：此方法使用 RabbitMQ 异步追加，不阻塞业务流程，性能更好
     * 适用于高频追加场景（如导入任务、批处理任务）
     * 
     * @param logId 日志ID
     * @param content 追加的内容
     */
    public void appendContent(String logId, String content) {
        // 添加时间戳
        String timestampedContent = StrUtil.format("[{}] {}", 
                                                  cn.hutool.core.date.DateUtil.now(), 
                                                  content);
        
        // 使用异步方式追加（不阻塞）
        appendLog(logId, timestampedContent);
    }
    
    /**
     * 追加内容到实时日志（同步，立即写入）
     * 
     * ⚠️ 仅在需要立即可见时使用（如关键节点、最终结果）
     * 大部分场景建议使用 appendContent() 异步方式
     * 
     * @param logId 日志ID
     * @param content 追加的内容
     * @throws RuntimeException 如果追加失败或 BusinessLogClient 未注入
     */
    public void appendContentSync(String logId, String content) {
        if (businessLogClient == null) {
            log.warn("BusinessLogClient 未注入，降级使用异步方式追加日志");
            appendContent(logId, content);
            return;
        }
        
        try {
            // 添加时间戳
            String timestampedContent = StrUtil.format("[{}] {}", 
                                                      cn.hutool.core.date.DateUtil.now(), 
                                                      content);
            
            // 调用简化接口 /append/{logId}
            ResultBody<Boolean> result = businessLogClient.appendLogSimple(logId, timestampedContent);
            
            if (!result.getSuccess()) {
                throw new RuntimeException("追加日志失败: " + result.getMessage());
            }
            
            log.debug("同步追加日志内容成功: logId={}", logId);
            
        } catch (Exception e) {
            log.error("同步追加日志内容失败: logId={}", logId, e);
            throw new RuntimeException("同步追加日志内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 追加进度信息（异步，格式化输出）
     * 
     * 推荐：用于高频进度更新，异步不阻塞业务
     * 
     * @param logId 日志ID
     * @param current 当前进度
     * @param total 总数
     * @param message 附加消息
     */
    public void appendProgress(String logId, long current, long total, String message) {
        double percentage = total > 0 ? (current * 100.0 / total) : 0;
        String content = StrUtil.format("进度: {}/{} ({:.2f}%) - {}", 
                                       current, total, percentage, message);
        appendContent(logId, content);  // 使用异步追加
    }
    
    /**
     * 追加进度信息（同步，立即可见）
     * 
     * ⚠️ 仅在需要立即看到进度时使用
     * 
     * @param logId 日志ID
     * @param current 当前进度
     * @param total 总数
     * @param message 附加消息
     */
    public void appendProgressSync(String logId, long current, long total, String message) {
        double percentage = total > 0 ? (current * 100.0 / total) : 0;
        String content = StrUtil.format("进度: {}/{} ({:.2f}%) - {}", 
                                       current, total, percentage, message);
        appendContentSync(logId, content);  // 使用同步追加
    }
    
    /**
     * 标记日志完成（异步）
     * 
     * 推荐：用于任务结束时记录最终结果
     * 
     * @param logId 日志ID
     * @param success 是否成功
     * @param summary 总结信息
     */
    public void finishRealtimeLog(String logId, boolean success, String summary) {
        String content = StrUtil.format("\n----------------------------------------\n" +
                                       "任务结束\n" +
                                       "状态: {}\n" +
                                       "结束时间: {}\n" +
                                       "总结: {}\n" +
                                       "----------------------------------------",
                                       success ? "✓ 成功" : "✗ 失败",
                                       cn.hutool.core.date.DateUtil.now(),
                                       summary);
        appendContent(logId, content);  // 使用异步追加
    }
    
    /**
     * 标记日志完成（同步，立即写入）
     * 
     * ⚠️ 用于需要确保最终结果立即可见的场景
     * 
     * @param logId 日志ID
     * @param success 是否成功
     * @param summary 总结信息
     */
    public void finishRealtimeLogSync(String logId, boolean success, String summary) {
        String content = StrUtil.format("\n----------------------------------------\n" +
                                       "任务结束\n" +
                                       "状态: {}\n" +
                                       "结束时间: {}\n" +
                                       "总结: {}\n" +
                                       "----------------------------------------",
                                       success ? "✓ 成功" : "✗ 失败",
                                       cn.hutool.core.date.DateUtil.now(),
                                       summary);
        appendContentSync(logId, content);  // 使用同步追加
    }
    
    // ==================== 文件上传功能 ====================
    
    /**
     * 上传本地日志文件（同步）
     * 读取本地文件内容并创建业务日志
     * 
     * ⚠️ 注意：此方法会读取整个文件到内存，不适合超大文件
     * 
     * @param filePath 本地文件路径
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param expireDays 过期天数
     * @param source 日志来源
     * @return logId - 业务日志ID
     * @throws RuntimeException 如果文件读取失败或上传失败
     */
    public String uploadLogFile(String filePath, String businessType, String businessId, 
                               Integer expireDays, String source) {
        try {
            // 读取文件内容
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + filePath);
            }
            
            if (file.length() > 10 * 1024 * 1024) { // 10MB 限制
                log.warn("文件过大: {} bytes，建议使用分块上传", file.length());
            }
            
            String content = cn.hutool.core.io.FileUtil.readUtf8String(file);
            
            // 创建日志
            return createLogSync(businessType, businessId, content, expireDays, source);
            
        } catch (Exception e) {
            log.error("上传日志文件失败: filePath={}, businessType={}, businessId={}", 
                     filePath, businessType, businessId, e);
            throw new RuntimeException("上传日志文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传本地日志文件（使用默认过期时间30天）
     * 
     * @param filePath 本地文件路径
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param source 日志来源
     * @return logId - 业务日志ID
     */
    public String uploadLogFile(String filePath, String businessType, String businessId, String source) {
        return uploadLogFile(filePath, businessType, businessId, 30, source);
    }
    
    /**
     * 上传日志内容（同步）
     * 直接使用字符串内容创建业务日志
     * 
     * @param content 日志内容
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param expireDays 过期天数
     * @param source 日志来源
     * @return logId - 业务日志ID
     */
    public String uploadLogContent(String content, String businessType, String businessId, 
                                  Integer expireDays, String source) {
        if (StrUtil.isBlank(content)) {
            throw new RuntimeException("日志内容不能为空");
        }
        
        return createLogSync(businessType, businessId, content, expireDays, source);
    }
    
    /**
     * 批量上传日志文件
     * 适用于批量归档历史日志文件
     * 
     * @param fileInfos 文件信息列表（文件路径 -> 业务信息映射）
     * @param source 日志来源
     * @return 成功上传的文件数量和对应的 logId 列表
     */
    public Map<String, String> uploadLogFiles(java.util.List<FileUploadInfo> fileInfos, String source) {
        if (fileInfos == null || fileInfos.isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        Map<String, String> results = new java.util.HashMap<>();
        int successCount = 0;
        
        for (FileUploadInfo info : fileInfos) {
            try {
                String logId = uploadLogFile(
                    info.getFilePath(), 
                    info.getBusinessType(), 
                    info.getBusinessId(), 
                    info.getExpireDays() != null ? info.getExpireDays() : 30,
                    source
                );
                results.put(info.getFilePath(), logId);
                successCount++;
                log.info("上传日志文件成功: {} -> logId={}", info.getFilePath(), logId);
            } catch (Exception e) {
                log.error("上传日志文件失败: {}", info.getFilePath(), e);
                results.put(info.getFilePath(), "ERROR: " + e.getMessage());
            }
        }
        
        log.info("批量上传日志文件完成: 总数={}, 成功={}, 失败={}", 
                fileInfos.size(), successCount, fileInfos.size() - successCount);
        
        return results;
    }
    
    /**
     * 追加本地文件内容到已存在的日志
     * 
     * @param logId 日志ID
     * @param filePath 本地文件路径
     * @throws RuntimeException 如果文件读取失败或追加失败
     */
    public void appendLogFile(String logId, String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + filePath);
            }
            
            String content = cn.hutool.core.io.FileUtil.readUtf8String(file);
            appendLog(logId, content);
            
            log.info("追加日志文件成功: logId={}, filePath={}", logId, filePath);
            
        } catch (Exception e) {
            log.error("追加日志文件失败: logId={}, filePath={}", logId, filePath, e);
            throw new RuntimeException("追加日志文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 文件上传信息
     */
    public static class FileUploadInfo {
        private String filePath;
        private String businessType;
        private String businessId;
        private Integer expireDays;
        
        public FileUploadInfo(String filePath, String businessType, String businessId) {
            this.filePath = filePath;
            this.businessType = businessType;
            this.businessId = businessId;
        }
        
        public FileUploadInfo(String filePath, String businessType, String businessId, Integer expireDays) {
            this.filePath = filePath;
            this.businessType = businessType;
            this.businessId = businessId;
            this.expireDays = expireDays;
        }
        
        public String getFilePath() { return filePath; }
        public String getBusinessType() { return businessType; }
        public String getBusinessId() { return businessId; }
        public Integer getExpireDays() { return expireDays; }
    }

    /**
     * 构建日志查询URL
     * 返回可以在浏览器中直接访问的日志查看地址
     * 
     * @param baseUrl 基础URL（如：http://localhost:8080）
     * @param logId 日志ID
     * @return 日志查看URL
     */
    public String buildLogViewUrl(String baseUrl, String logId) {
        return StrUtil.format("{}/businessLog/get/{}.log", baseUrl, logId);
    }

    /**
     * 构建日志查询URL（通过业务类型和业务ID）
     * 
     * @param baseUrl 基础URL
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 日志查看URL（需要结合查询接口使用）
     */
    public String buildLogQueryUrl(String baseUrl, String businessType, String businessId) {
        return StrUtil.format("{}/businessLog/query?businessType={}&businessId={}", 
                             baseUrl, businessType, businessId);
    }
}


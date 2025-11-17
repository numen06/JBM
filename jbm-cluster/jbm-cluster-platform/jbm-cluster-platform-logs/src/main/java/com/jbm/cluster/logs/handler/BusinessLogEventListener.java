package com.jbm.cluster.logs.handler;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.api.model.log.BusinessLogEvent;
import com.jbm.cluster.logs.service.BusinessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 业务日志事件监听器
 * 处理来自其他服务通过消息队列发送的业务日志事件
 * 
 * 配置说明：
 * 1. 在 application.yml 中配置 Spring Cloud Stream 绑定
 * 2. 配置示例：
 * spring:
 *   cloud:
 *     stream:
 *       bindings:
 *         businessLog-in-0:
 *           destination: cloud.business.log
 *           group: business-log-group
 *           consumer:
 *             max-attempts: 3
 *       function:
 *         definition: businessLog
 * 
 * @author wesley
 */
@Slf4j
@Service
public class BusinessLogEventListener {

    @Autowired
    private BusinessLogService businessLogService;

    /**
     * 业务日志事件消费者
     * 
     * @return Consumer
     */
    @Bean
    public Consumer<Message<BusinessLogEvent>> businessLog() {
        return message -> {
            try {
                BusinessLogEvent event = message.getPayload();
                log.info("接收到业务日志事件: type={}, logId={}, businessType={}, businessId={}",
                         event.getEventType(), event.getLogId(), event.getBusinessType(), event.getBusinessId());
                
                // 根据事件类型处理
                switch (event.getEventType()) {
                    case CREATE:
                        handleCreateEvent(event);
                        break;
                    case APPEND:
                        handleAppendEvent(event);
                        break;
                    case DELETE:
                        handleDeleteEvent(event);
                        break;
                    case UPDATE_EXPIRE:
                        handleUpdateExpireEvent(event);
                        break;
                    case GENERATE_URL:
                        handleGenerateUrlEvent(event);
                        break;
                    case QUERY:
                        handleQueryEvent(event);
                        break;
                    default:
                        log.warn("未知的业务日志事件类型: {}", event.getEventType());
                }
                
            } catch (Exception e) {
                log.error("处理业务日志事件失败", e);
                // 这里可以选择重新抛出异常，让消息队列进行重试
                // throw new RuntimeException("处理业务日志事件失败", e);
            }
        };
    }

    /**
     * 处理创建日志事件
     */
    private void handleCreateEvent(BusinessLogEvent event) {
        try {
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setBusinessType(event.getBusinessType());
            form.setBusinessId(event.getBusinessId());
            form.setContent(event.getContent());
            form.setExpireDays(event.getExpireDays() != null ? event.getExpireDays() : 30);
            form.setSource(event.getSource());
            
            String logId = businessLogService.createLog(form);
            log.info("创建业务日志成功: logId={}, businessType={}, businessId={}", 
                    logId, event.getBusinessType(), event.getBusinessId());
            
            // 这里可以通过回调或其他方式返回logId给调用方
            // 例如：发送响应消息到响应队列
            
        } catch (Exception e) {
            log.error("创建业务日志失败: businessType={}, businessId={}", 
                     event.getBusinessType(), event.getBusinessId(), e);
            throw new RuntimeException("创建业务日志失败", e);
        }
    }

    /**
     * 处理追加日志事件
     */
    private void handleAppendEvent(BusinessLogEvent event) {
        try {
            AppendBusinessLogForm form = new AppendBusinessLogForm();
            
            // 优先使用logId，如果没有则尝试通过businessType和businessId查询
            if (StrUtil.isNotBlank(event.getLogId())) {
                form.setLogId(event.getLogId());
            } else if (StrUtil.isNotBlank(event.getBusinessType()) && StrUtil.isNotBlank(event.getBusinessId())) {
                // 通过businessType和businessId查询logId
                String logId = businessLogService.getLogIdByBusinessId(event.getBusinessType(), event.getBusinessId());
                if (StrUtil.isBlank(logId)) {
                    log.error("未找到对应的业务日志: businessType={}, businessId={}", 
                             event.getBusinessType(), event.getBusinessId());
                    return;
                }
                form.setLogId(logId);
            } else {
                log.error("追加日志失败：logId和businessType/businessId都为空");
                return;
            }
            
            form.setContent(event.getContent());
            
            boolean success = businessLogService.appendLog(form);
            if (success) {
                log.info("追加业务日志成功: logId={}", form.getLogId());
            } else {
                log.error("追加业务日志失败: logId={}", form.getLogId());
            }
            
        } catch (Exception e) {
            log.error("追加业务日志异常: logId={}", event.getLogId(), e);
        }
    }

    /**
     * 处理删除日志事件
     */
    private void handleDeleteEvent(BusinessLogEvent event) {
        try {
            String logId = event.getLogId();
            
            // 如果没有logId，尝试通过businessType和businessId查询
            if (StrUtil.isBlank(logId) && StrUtil.isNotBlank(event.getBusinessType()) 
                && StrUtil.isNotBlank(event.getBusinessId())) {
                logId = businessLogService.getLogIdByBusinessId(event.getBusinessType(), event.getBusinessId());
            }
            
            if (StrUtil.isBlank(logId)) {
                log.error("删除日志失败：无法确定logId");
                return;
            }
            
            boolean success = businessLogService.deleteLog(logId);
            if (success) {
                log.info("删除业务日志成功: logId={}", logId);
            } else {
                log.error("删除业务日志失败: logId={}", logId);
            }
            
        } catch (Exception e) {
            log.error("删除业务日志异常: logId={}", event.getLogId(), e);
        }
    }

    /**
     * 处理更新过期时间事件
     */
    private void handleUpdateExpireEvent(BusinessLogEvent event) {
        try {
            boolean success = businessLogService.updateExpireTime(event.getLogId(), event.getExpireDays());
            if (success) {
                log.info("更新业务日志过期时间成功: logId={}, expireDays={}", 
                        event.getLogId(), event.getExpireDays());
            } else {
                log.error("更新业务日志过期时间失败: logId={}", event.getLogId());
            }
            
        } catch (Exception e) {
            log.error("更新业务日志过期时间异常: logId={}", event.getLogId(), e);
        }
    }

    /**
     * 处理生成临时URL事件
     */
    private void handleGenerateUrlEvent(BusinessLogEvent event) {
        try {
            // 生成临时URL
            // 这里可以调用相关服务生成URL，并通过回调返回给调用方
            log.info("生成业务日志临时URL: logId={}, expireMinutes={}", 
                    event.getLogId(), event.getExpireMinutes());
            
            // 实际实现中，可以调用 businessLogService.generateTemporaryUrlParams
            // 然后将结果发送到响应队列
            
        } catch (Exception e) {
            log.error("生成业务日志临时URL异常: logId={}", event.getLogId(), e);
        }
    }

    /**
     * 处理查询日志事件
     */
    private void handleQueryEvent(BusinessLogEvent event) {
        try {
            // 查询日志
            // 这里可以根据logId或businessType+businessId查询日志
            // 并通过回调返回给调用方
            log.info("查询业务日志: logId={}, businessType={}, businessId={}", 
                    event.getLogId(), event.getBusinessType(), event.getBusinessId());
            
            // 实际实现中，可以调用 businessLogService 的查询方法
            // 然后将结果发送到响应队列
            
        } catch (Exception e) {
            log.error("查询业务日志异常", e);
        }
    }
}


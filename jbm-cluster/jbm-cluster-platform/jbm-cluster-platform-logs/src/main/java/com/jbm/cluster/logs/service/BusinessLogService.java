package com.jbm.cluster.logs.service;

import com.jbm.cluster.logs.entity.BusinessLog;
import com.jbm.cluster.logs.form.AppendBusinessLogForm;
import com.jbm.cluster.logs.form.BusinessLogForm;
import com.jbm.cluster.logs.form.CreateBusinessLogForm;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;
import java.util.Map;

/**
 * 业务日志服务接口
 * 
 * @author wesley
 */
public interface BusinessLogService {
    
    /**
     * 创建业务日志并生成logId
     * 
     * @param form 创建业务日志表单
     * @return 返回生成的业务日志ID
     */
    String createLog(CreateBusinessLogForm form);
    
    /**
     * 追加日志内容到已存在的业务日志
     * 
     * @param form 追加日志表单
     * @return 是否追加成功
     */
    boolean appendLog(AppendBusinessLogForm form);
    
    /**
     * 根据logId查询业务日志（多行格式）
     * 
     * @param logId 业务日志ID
     * @return 业务日志列表
     */
    List<BusinessLog> getLogByIdMultiLine(String logId);
    
    /**
     * 根据logId查询业务日志（整个文件格式，内容拼接）
     * 
     * @param logId 业务日志ID
     * @param formatted 是否格式化（true: 添加头部信息和行号，false: 仅返回原始日志内容）
     * @return 完整的业务日志内容
     */
    String getLogByIdFullContent(String logId, Boolean formatted);
    
    /**
     * 根据logId查询业务日志（整个文件格式，内容拼接）
     * 默认格式化输出
     * 
     * @param logId 业务日志ID
     * @return 完整的业务日志内容
     */
    default String getLogByIdFullContent(String logId) {
        return getLogByIdFullContent(logId, true);
    }
    
    /**
     * 根据logId和行号范围查询业务日志（类似tail -n功能）
     * 
     * @param logId 业务日志ID
     * @param startLine 起始行号（从1开始）
     * @param endLine 结束行号（-1表示到最后一行）
     * @return 日志列表
     */
    List<BusinessLog> getLogByLineRange(String logId, Integer startLine, Integer endLine);
    
    /**
     * 获取日志总行数
     * 
     * @param logId 业务日志ID
     * @return 总行数
     */
    Integer getLogTotalLines(String logId);
    
    /**
     * 分页查询业务日志
     * 
     * @param form 查询表单
     * @return 分页数据
     */
    DataPaging<BusinessLog> queryLogs(BusinessLogForm form);
    
    /**
     * 根据logId删除业务日志
     * 
     * @param logId 业务日志ID
     * @return 是否删除成功
     */
    boolean deleteLog(String logId);
    
    /**
     * 更新业务日志的过期时间
     * 
     * ⚠️ 限制使用：由于日志按过期时间存储在OpenObserve的不同流中，
     * 更新过期时间不会立即迁移数据。原始日志仍在原流中，直到原流的TTL到期。
     * 建议：在创建日志时设置正确的过期时间。
     * 
     * @param logId 业务日志ID
     * @param expireDays 过期天数
     * @return 是否更新成功
     */
    boolean updateExpireTime(String logId, Integer expireDays);
    
    /**
     * 清理过期的业务日志（已废弃）
     * 
     * @deprecated 过期管理已由OpenObserve自动处理（通过流的保留策略TTL），
     * 此方法仅用于业务层面的状态标记，实际数据删除由OpenObserve自动完成。
     * 
     * @return 清理的日志数量（实际为标记数量）
     */
    @Deprecated
    int cleanExpiredLogs();
    
    /**
     * 生成日志的临时访问URL参数（类似OSS签名URL）
     * 
     * @param logId 业务日志ID
     * @param expireMinutes 过期时间（分钟），默认60分钟
     * @return 包含URL参数的Map（expires, accessKeyId, signature等）
     */
    Map<String, String> generateTemporaryUrlParams(String logId, Integer expireMinutes);
    
    /**
     * 通过临时token访问日志内容
     * 
     * @param logId 业务日志ID
     * @param token 临时访问token
     * @return 日志内容
     */
    String getLogByToken(String logId, String token);
}


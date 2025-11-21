package com.jbm.cluster.logs.service;

import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.entitys.log.BusinessLogStageSnapshot;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogStageUpdateForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.api.form.log.InitBusinessLogStageForm;
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
    
    /**
     * 通过业务类型和业务ID获取日志ID
     * 用于消息队列场景，当只有业务信息而没有logId时使用
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 日志ID，如果不存在则返回null
     */
    String getLogIdByBusinessId(String businessType, String businessId);

    /**
     * 初始化业务日志阶段
     *
     * @param form 阶段配置
     * @return 阶段快照
     */
    BusinessLogStageSnapshot initStages(InitBusinessLogStageForm form);

    /**
     * 更新阶段进度
     *
     * @param form 更新表单
     * @return 最新快照
     */
    BusinessLogStageSnapshot updateStage(BusinessLogStageUpdateForm form);

    /**
     * 查询阶段快照
     *
     * @param logId 日志ID
     * @return 阶段快照
     */
    BusinessLogStageSnapshot getStageSnapshot(String logId);
}


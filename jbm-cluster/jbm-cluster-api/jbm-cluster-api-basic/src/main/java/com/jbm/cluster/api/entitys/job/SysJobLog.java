package com.jbm.cluster.api.entitys.job;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * 定时任务调度日志表 sys_job_log
 *
 * @author wesley
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("系统任务日志")
public class SysJobLog extends MasterDataEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("日志序号")
    private Long jobLogId;

    /**
     * 任务名称
     */
    @ApiModelProperty("任务名称")
    private String jobName;

    /**
     * 任务组名
     */
    @ApiModelProperty("任务组名")
    private String jobGroup;

    /**
     * 调用目标字符串
     */
    @ApiModelProperty("调用目标字符串")
    private String invokeTarget;

    /**
     * 日志信息
     */
    @ApiModelProperty("日志信息")
    private String jobMessage;

    /**
     * 执行状态（0正常 1失败）
     */
    @ApiModelProperty("执行状态:0=正常,1=失败")
    private String status;

    /**
     * 异常信息
     */
    @ApiModelProperty("异常信息")
    private String exceptionInfo;

    /**
     * 开始时间
     */
    @ApiModelProperty("开始时间")
    private Date startTime;

    /**
     * 停止时间
     */
    @ApiModelProperty("结束时间")
    private Date stopTime;

}

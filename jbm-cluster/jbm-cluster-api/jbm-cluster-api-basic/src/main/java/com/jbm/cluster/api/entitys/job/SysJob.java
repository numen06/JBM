package com.jbm.cluster.api.entitys.job;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.jbm.cluster.api.constants.job.MisfirePolicy;
import com.jbm.cluster.api.constants.job.ScheduleStauts;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 定时任务调度表 sys_job
 *
 * @author wesley
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("系统任务")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"jobName", "jobGroup"}))
public class SysJob extends MasterDataEntity {

    /**
     * 任务ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("任务序号")
    private Long jobId;

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
     * cron执行表达式
     */
    @ApiModelProperty("执行表达式")
    private String cronExpression;

    /**
     * cron计划策略
     */
    @ApiModelProperty("计划策略:0=默认,1=立即触发执行,2=触发一次执行,3=不触发立即执行")
    private MisfirePolicy misfirePolicy;

    /**
     * 是否并发执行（0允许 1禁止）
     */
    @ApiModelProperty("并发执行:1=允许,0=禁止")
    private Boolean concurrent;

    /**
     * 任务状态（0正常 1暂停）
     */
    @ApiModelProperty("任务状态:0=正常,1=暂停")
    private ScheduleStauts status;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty(value = "描述")
    private String description;
}
package com.jbm.cluster.api.entitys.job.rule;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @description 
 * @author scolin
 * @date 2025/10/22 11:03
 */
@Data
@Table
@Entity
@ApiModel("节点执行记录")
public class NodeExecution extends MasterDataEntity {
    @TableField(exist = false)
    private Long id;
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private Long appId;
    @TableField(exist = false)
    private Long parentId;
    @TableField(exist = false)
    private Integer level;
    @TableField(exist = false)
    private String leafPath;

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    @Column(name = "id")
    @ApiModelProperty("节点执行记录id")
    private String nodeExecutionId;

    @ApiModelProperty("流程实例id")
    private String processInstanceId;
    @ApiModelProperty("节点id")
    private String nodeId;
    @ApiModelProperty("节点类型")
    private String nodeType;
    @ApiModelProperty("节点状态")
    private String status;
    @Lob
    @ApiModelProperty("节点输入数据")
    private String inputData;
    @Lob
    @ApiModelProperty("节点输出数据")
    private String outputData;
    @Lob
    @ApiModelProperty("错误信息")
    private String errorMessage;

    @CreationTimestamp
    @ApiModelProperty("开始时间")
    private LocalDateTime startedAt;
    @ApiModelProperty("结束时间")
    private LocalDateTime completedAt;
}

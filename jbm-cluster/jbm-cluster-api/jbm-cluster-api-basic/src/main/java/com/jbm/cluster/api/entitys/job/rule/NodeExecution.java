package com.jbm.cluster.api.entitys.job.rule;

import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty("节点执行记录id")
    private String id;

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

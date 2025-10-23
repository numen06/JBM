package com.jbm.cluster.api.entitys.job.rule;

import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
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
    private String id;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    private String nodeId;
    private String nodeType;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, WAITING
    @Lob
    private String inputData; // 节点输入数据
    @Lob
    private String outputData; // 节点输出数据
    @Lob
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

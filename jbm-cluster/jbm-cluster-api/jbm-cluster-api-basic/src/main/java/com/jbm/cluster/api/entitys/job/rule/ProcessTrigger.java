package com.jbm.cluster.api.entitys.job.rule;

import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:07
 */
@Data
@Table
@Entity
@ApiModel("流程触发器")
public class ProcessTrigger extends MasterDataEntity {
    @Id
    private String id;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "node_id")
    private String nodeId;

    private String triggerType; // MQTT, HTTP, MANUAL
    private String triggerKey; // MQTT topic或其他标识
    private String status; // WAITING, TRIGGERED
    @Lob
    private String triggerData; // 触发时的数据

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime triggeredAt;
}

package com.jbm.cluster.api.entitys.job.rule;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
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
    private String triggerId;

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

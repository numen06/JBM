package com.jbm.cluster.api.entitys.job.rule;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    @Column(name = "id")
    private String triggerId;

    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

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

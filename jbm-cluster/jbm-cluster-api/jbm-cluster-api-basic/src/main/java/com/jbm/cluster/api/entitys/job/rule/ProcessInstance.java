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
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author scolin
 * @description
 * @date 2025/10/22 11:01
 */
@Data
@Table
@Entity
@ApiModel("流程实例")
public class ProcessInstance extends MasterDataEntity {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    @Column(name = "id")
    private String instanceId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Column(name = "rule_definition_id")
    private Long ruleDefinitionId;
    // 规则名称
    private String ruleName;
    // 规则内容JSON
    @Lob
    private String ruleContent;
    // RUNNING, COMPLETED, FAILED, WAITING
    private String status;
    // JSON格式的输入参数
    @Lob
    private String inputParams;
    // JSON格式的输出参数
    @Lob
    private String outputParams;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 节点执行列表，不映射到数据库
    @Transient
    @TableField(exist = false)
    private List<NodeExecution> nodeExecutions;
}
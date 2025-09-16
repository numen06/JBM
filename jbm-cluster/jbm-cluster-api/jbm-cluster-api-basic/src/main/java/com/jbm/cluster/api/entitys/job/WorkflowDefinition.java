package com.jbm.cluster.api.entitys.job;

import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author scolin
 * @description 工作流实体
 * @date 2025/8/25 13:40
 */
@Data
@Table
@Entity
@ApiModel("工作流实体")
public class WorkflowDefinition extends MasterDataIdEntity implements Cloneable{
    private static final long serialVersionUID = 1L;
    private static WorkflowDefinition workflowDefinition = new WorkflowDefinition();

    @ApiModelProperty("工作流编号")
    private String workflowCode;
    @ApiModelProperty("workflowName")
    private String workflowName;
    @ApiModelProperty("组")
    private String workflowGroup;
    @ApiModelProperty("工作流描述")
    private String workflowDesc;
    @ApiModelProperty("规则状态 为0未启用")
    private Boolean workflowStatus;
    @Lob
    @ApiModelProperty("原始JSON内容")
    private String jsonContent;
    @Lob
    @ApiModelProperty("BPMN XML内容")
    private String bpmnContent;
    @ApiModelProperty("版本号")
    private String version;

    public static WorkflowDefinition getInstance() {
        try {
            return (WorkflowDefinition)workflowDefinition.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new WorkflowDefinition();
    }

}

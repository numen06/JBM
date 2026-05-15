package com.jbm.cluster.api.entitys.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author wesley
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName
@ApiModel("Web反向推送")
public class WebhookTask extends MasterDataEntity {
    @TableField(exist = false)
    private Long id;
    @TableField(exist = false)
    private String code;
    @TableField(exist = false)
    private Long parentId;
    @TableField(exist = false)
    private Integer level;
    @TableField(exist = false)
    private String leafPath;

    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty(value = "事件ID")
    private String eventId;
    @ApiModelProperty(value = "执行URL")
    private String taskUrl;
    @ApiModelProperty(value = "请求方法")
    private String taskMethod;
    @ApiModelProperty("请求体")
    @Column(columnDefinition = "TEXT")
    private String request;
    @ApiModelProperty("返回体")
    @Column(columnDefinition = "TEXT")
    private String response;
    @ApiModelProperty("事件状态")
    private Integer httpStatus;
    @ApiModelProperty("重试次数")
    private Integer retryNumber;
    @ApiModelProperty("错误信息")
    private String errorMsg;
    @ApiModelProperty("任务状态")
    private String status;


}

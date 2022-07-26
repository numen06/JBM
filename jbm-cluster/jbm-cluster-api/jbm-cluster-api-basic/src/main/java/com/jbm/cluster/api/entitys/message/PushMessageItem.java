package com.jbm.cluster.api.entitys.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

/**
 * 站内信
 *
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-04 21:21
 **/
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName
@ApiModel("消息推送项")
public class PushMessageItem extends MasterDataEntity {
    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "推送消息ID")
    private String msgId;
    @ApiModelProperty("消息内容ID")
    private Long msgBodyId;
    @ApiModelProperty("接收者ID")
    private Long recUserId;
    @ApiModelProperty("发送者ID")
    private Long sendUserId;
    @ApiModelProperty("发送状态")
    @Enumerated(EnumType.STRING)
    private PushStatus pushStatus;
    @ApiModelProperty("发送渠道")
    @Enumerated(EnumType.STRING)
    private PushWay pushWay;
    @ApiModelProperty("是否已读")
    private Boolean readFlag;

}

package com.jbm.cluster.api.entitys.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.framework.masterdata.code.annotation.BussinessGroup;
import com.jbm.framework.masterdata.code.annotation.IgnoreGeneate;
import com.jbm.framework.masterdata.code.constants.CodeType;
import com.jbm.framework.masterdata.usage.entity.MasterDataCodeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
@ApiModel("推送消息内容")
@IgnoreGeneate(value = {CodeType.controller})
@BussinessGroup(businessName = "PushMessage")
public class PushMessageBody extends MasterDataCodeEntity {

    @ApiModelProperty("发送者ID")
    private Long sendUserId;
    //    @ApiModelProperty("接收者ID")
//    private Long recUserId;
    @ApiModelProperty("标题")
    private String title;
    @ApiModelProperty("标签组")
    private String tags;
    @ApiModelProperty("内容")
    private String content;
    @ApiModelProperty("消息类型")
    @Enumerated(EnumType.STRING)
    private PushMsgType type;
    @ApiModelProperty("等级")
    private Integer level;
    //    @ApiModelProperty("逻辑删除标志")
//    @TableLogic
//    @TableField(select = false)
//    private Integer deleteFlag;
    @ApiModelProperty("扩展字段")
    private String extend;

}

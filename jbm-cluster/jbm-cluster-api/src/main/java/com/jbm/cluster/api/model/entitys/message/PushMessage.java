package com.jbm.cluster.api.model.entitys.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataCodeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

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
@TableName("push_message")
@ApiModel("站内信")
public class PushMessage extends MasterDataCodeEntity implements Notification {

    /**
     * 用户id
     */
    @ApiModelProperty("发送者ID")
    private Long sendUserId;
    /**
     * 用户id
     */
    @ApiModelProperty("接收者ID")
    private Long recUserId;
    /**
     * 标题
     */
    @ApiModelProperty("标题")
    private String title;
    /**
     * 标签组：a,b,c
     */
    @ApiModelProperty("标签组")
    private String tags;
    /**
     * 内容
     */
    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("消息类型")
    private String type;

    @ApiModelProperty("等级")
    private Integer level;
//    /**
//     * 是否发送邮件：-1不发送 0 待发送，1已发送
//     */
//    @ApiModelProperty("是否发送邮件")
//    private MessageSendStatus emailStatus;
//    /**
//     * 是否发送短信：-1不发送 0 待发送，1已发送
//     */
//    @ApiModelProperty("否发送短信")
//    private MessageSendStatus smsStatus;
    /**
     * 读标志：0未读，1已读
     */
    @ApiModelProperty("已读标志")
    private Integer readFlag;
    /**
     * 删除标志：N未删除，Y已删除
     */
    @ApiModelProperty("逻辑删除标志")
    private Integer deleteFlag;
    /**
     * 扩展字段
     */
    @ApiModelProperty("扩展字段")
    private String extend;
    /**
     * 业务类型
     */
    @ApiModelProperty("业务编码")
    private String bizCode;
}

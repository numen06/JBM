package com.jbm.cluster.api.entitys.push;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 邮件推送设置
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName
@ApiModel("消息推送项")
public class EmailPushConfig extends MasterDataIdEntity {

    private String host;
    private String username;
    private String password;
    private String port;

}

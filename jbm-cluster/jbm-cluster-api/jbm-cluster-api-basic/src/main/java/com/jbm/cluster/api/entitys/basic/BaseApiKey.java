package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

/**
 * 第三方 API 访问凭证（独立于业务应用 base_app）
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("API Key")
@TableName("base_api_key")
public class BaseApiKey extends MasterDataEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("API Key ID")
    private Long keyId;

    @ApiModelProperty("所属开发者ID")
    private Long developerId;

    @ApiModelProperty("所属业务应用ID（null=开发者个人Key）")
    private Long bizAppId;

    @ApiModelProperty("AccessKey ID")
    private String apiKey;

    @ApiModelProperty("AccessKey Secret（BCrypt）")
    private String secretKey;

    @ApiModelProperty("RSA公钥")
    @Column(columnDefinition = "TEXT")
    private String publicKey;

    @ApiModelProperty("RSA私钥")
    @Column(columnDefinition = "TEXT")
    private String privateKey;

    @ApiModelProperty("名称")
    private String keyName;

    @ApiModelProperty("描述")
    private String keyDesc;

    @ApiModelProperty("第三方客户名称")
    private String clientName;

    @ApiModelProperty("授权模块范围（businessScope逗号分隔，可选）")
    private String scopeModules;

    @ApiModelProperty("过期时间")
    private Date expireTime;

    @ApiModelProperty("状态: 0-禁用 1-启用")
    private Integer status;

    @ApiModelProperty("撤销时间")
    private Date revokeTime;

    @ApiModelProperty("最后使用时间")
    private Date lastUsedTime;
}

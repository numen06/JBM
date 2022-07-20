package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

/**
 * 系统用户-登录账号
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("用户实名认证")
@TableName
@Table(indexes = {@Index(name = "userIdIndex", columnList = "userId", unique = true)})
public class BaseUserCertification extends MasterDataIdEntity {

    /**
     * 系统用户Id
     */
    @ApiModelProperty(value = "系统用户Id")
    private Long userId;

    /**
     * 身份证号
     */
    @ApiModelProperty(value = "身份证号")
    private String idCard;


    @ApiModelProperty(value = "证件类型")
    private String cardType;

    /**
     * 起始有效期
     */
    @ApiModelProperty(value = "起始有效期")
    private Date expirationDate;

    /**
     * 到期有效期
     */
    @ApiModelProperty(value = "到期有效期")
    private Date effectiveDate;

    /**
     * 实名类型,1:个人;2:企业
     */
    @ApiModelProperty(value = "实名类型,1:个人;2:企业")
    private Integer certificationType;

    /**
     * 实名认证状态
     */
    @ApiModelProperty(value = "实名认证状态")
    private Integer status;

    /**
     * 人脸照片
     */
    @Column(columnDefinition = "mediumtext")
    @ApiModelProperty(value = "人脸照片")
    private String faceImage;

    /**
     * 指纹信息
     */
    @ApiModelProperty(value = "指纹信息")
    private String fingerprint;

}

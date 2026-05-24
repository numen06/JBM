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

import javax.persistence.Entity;
import java.util.Date;

/**
 * 用户-组织数据授权（跨组织访问）
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_user_org")
@ApiModel("用户组织授权")
public class BaseUserOrg extends MasterDataEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("授权组织ID")
    private Long orgId;

    @ApiModelProperty("过期时间，空表示永久")
    private Date expireTime;
}

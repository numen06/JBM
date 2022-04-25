package com.jbm.cluster.api.model;

import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-27 23:24
 **/
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AuthorityExt extends MasterDataEntity {

    /**
     * 权限ID
     */
    @Id
    @ApiModelProperty("权限ID")
    private Long authorityId;

    /**
     * 过期时间:null表示长期
     */
    @ApiModelProperty("过期时间")
    private Date expireTime;
}

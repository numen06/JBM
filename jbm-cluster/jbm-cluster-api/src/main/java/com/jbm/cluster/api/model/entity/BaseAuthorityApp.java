package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

/**
 * 系统权限-应用关联
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_authority_app")
public class BaseAuthorityApp extends MasterDataIdEntity {
    /**
     * 权限ID
     */
    private Long authorityId;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 过期时间:null表示长期
     */
    private Date expireTime;

    private static final long serialVersionUID = 1L;
}

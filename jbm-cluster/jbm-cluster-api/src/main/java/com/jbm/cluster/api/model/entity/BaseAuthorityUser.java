package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.util.Date;

/**
 * 系统权限-用户关联
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_authority_user")
public class BaseAuthorityUser extends MasterDataEntity {
    /**
     * 权限ID
     */
    private Long authorityId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 过期时间
     */
    private Date expireTime;

    private static final long serialVersionUID = 1L;
}

package com.jbm.cluster.common.mysql.mapper;

import com.jbm.cluster.api.entitys.basic.BaseAuthorityApikey;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import org.apache.ibatis.annotations.Param;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseAuthorityApikeyMapper extends SuperMapper<BaseAuthorityApikey> {

    List<OpenAuthority> selectAuthorityByKeyId(@Param("keyId") Long keyId);

    List<Long> selectAuthorityIdsByKeyId(@Param("keyId") Long keyId);
}

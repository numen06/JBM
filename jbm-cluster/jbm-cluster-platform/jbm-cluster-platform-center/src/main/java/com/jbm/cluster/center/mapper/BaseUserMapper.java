package com.jbm.cluster.center.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface BaseUserMapper extends SuperMapper<BaseUser> {

    /**
     * 查询列表
     *
     * @param form
     * @return
     */
    List<BaseUser> selectData(@Param("form") BaseUserForm form);

    /**
     * 查询分页列表
     *
     * @param form
     * @param page
     * @return
     */
    IPage<BaseUser> selectData(@Param("form") BaseUserForm form, Page page);

}

package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.common.mysql.mapper.BaseUserMapper;
import com.jbm.cluster.common.mysql.service.BaseUserService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BaseUserServiceImpl extends MasterDataServiceImpl<BaseUser> implements BaseUserService {

    @Autowired
    protected BaseUserMapper baseUserMapper;

    @Override
    public List<BaseUser> selectUserRows(BaseUserForm baseUserForm) {
        return baseUserMapper.selectData(baseUserForm);
    }

    @Override
    public DataPaging<BaseUser> selectUserRows(BaseUserForm baseUserForm, PageForm pageForm) {
        return super.selectPageList(pageForm, (page) -> baseUserMapper.selectData(baseUserForm, page));
    }

    @Override
    public List<BaseUser> findAllList() {
        return baseUserMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public BaseUser getUserById(Long userId) {
        return baseUserMapper.selectById(userId);
    }

    @Override
    public BaseUser getUserByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        BaseUser baseUser = new BaseUser();
        baseUser.setMobile(phone);
        return this.selectEntity(baseUser);
    }

    @Override
    public BaseUser getUserByUsername(String username) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseUser::getUserName, username);
        return baseUserMapper.selectOne(queryWrapper);
    }

    @Override
    public List<BaseUser> getUsersByIds(List<Long> ids) {
        QueryWrapper<BaseUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(BaseUser::getUserId, ids);
        return this.selectEntitys(queryWrapper);
    }
}
package com.jbm.cluster.center.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseRoleUser;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseRoleForm;
import com.jbm.cluster.center.mapper.BaseRoleMapper;
import com.jbm.cluster.center.mapper.BaseRoleUserMapper;
import com.jbm.cluster.center.service.BaseRoleService;
import com.jbm.cluster.center.service.BaseUserService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;


/**
 * @author wesley.zhang
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseRoleServiceImpl extends MasterDataServiceImpl<BaseRole> implements BaseRoleService {
    @Autowired
    private BaseRoleMapper baseRoleMapper;
    @Autowired
    private BaseRoleUserMapper baseRoleUserMapper;
    @Autowired
    private BaseUserService baseUserService;

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseRole> findListPage(PageRequestBody pageRequestBody) {
//        BaseRole query = pageRequestBody.tryGet(BaseRole.class);
//        QueryWrapper<BaseRole> queryWrapper = new QueryWrapper();
//        queryWrapper.lambda()
//                .likeRight(ObjectUtils.isNotEmpty(query.getRoleCode()), BaseRole::getRoleCode, query.getRoleCode())
//                .likeRight(ObjectUtils.isNotEmpty(query.getRoleName()), BaseRole::getRoleName, query.getRoleName());
//        queryWrapper.orderByDesc("create_time");
//        return this.selectEntitys(pageRequestBody.getPageParams(), queryWrapper);
        PageForm pageForm = pageRequestBody.getPageForm();
        pageForm.setSortRule("base_role.create_time");
        BaseRoleForm roleForm = pageRequestBody.tryGet(BaseRoleForm.class);
        roleForm.setUserId(LoginHelper.isAdmin() ? null : LoginHelper.getUserId());
        // 根据时间过滤
        if (ObjectUtil.isNotEmpty(roleForm.getDateRange())) {
            roleForm.setBeginTime(roleForm.getDateRange()[0]);
            roleForm.setEndTime(roleForm.getDateRange()[1]);
        }
        return super.selectPageList(pageForm, (page) -> this.baseRoleMapper.selectData(roleForm, page));
    }

    /**
     * 查询列表
     *
     * @return
     */
    @Override
    public List<BaseRole> findAllList() {
//        List<BaseRole> list = baseRoleMapper.selectList(new QueryWrapper<>());
//        return list;
        BaseRoleForm roleForm = new BaseRoleForm();
        roleForm.setUserId(LoginHelper.getUserId());
        return this.baseRoleMapper.selectData(roleForm);
    }

    /**
     * 获取角色信息
     *
     * @param roleId
     * @return
     */
    @Override
    public BaseRole getRole(Long roleId) {
        return baseRoleMapper.selectById(roleId);
    }

    /**
     * 添加角色
     *
     * @param role 角色
     * @return
     */
    @Override
    public BaseRole addRole(BaseRole role) {
//        if (isExist(role.getRoleCode())) {
//            throw new ServiceException(String.format("%s编码已存在!", role.getRoleCode()));
//        }
        if (role.getStatus() == null) {
            role.setStatus(JbmConstants.ENABLED);
        }
        if (role.getIsPersist() == null) {
            role.setIsPersist(JbmConstants.DISABLED);
        }
        this.saveEntity(role);
        return role;
    }

    /**
     * 更新角色
     *
     * @param role 角色
     * @return
     */
    @Override
    public BaseRole updateRole(BaseRole role) {
        BaseRole saved = getRole(role.getRoleId());
        if (role == null) {
            throw new ServiceException("信息不存在!");
        }
        if (!saved.getRoleCode().equals(role.getRoleCode())) {
            // 和原来不一致重新检查唯一性
            if (isExist(role.getRoleCode())) {
                throw new ServiceException(String.format("%s编码已存在!", role.getRoleCode()));
            }
        }
        role.setUpdateTime(new Date());
        baseRoleMapper.updateById(role);
        return role;
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return
     */
    @Override
    public void removeRole(Long roleId) {
        if (roleId == null) {
            return;
        }
        BaseRole role = getRole(roleId);
        if (role != null && role.getIsPersist().equals(JbmConstants.ENABLED)) {
            throw new ServiceException(String.format("保留数据,不允许删除"));
        }
        Long count = getCountByRole(roleId);
        if (count > 0) {
            throw new ServiceException("该角色下存在授权人员,不允许删除!");
        }
        baseRoleMapper.deleteById(roleId);
    }

    /**
     * 检测角色编码是否存在
     *
     * @param roleCode
     * @return
     */
    @Override
    public Boolean isExist(String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            throw new ServiceException("roleCode不能为空!");
        }
        QueryWrapper<BaseRole> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRole::getRoleCode, roleCode);
        return baseRoleMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 用户添加角色
     *
     * @param userId
     * @param roles
     * @return
     */
    @Override
    public void saveUserRoles(Long userId, String... roles) {
        if (userId == null || roles == null) {
            return;
        }
        BaseUser user = baseUserService.getUserById(userId);
        if (user == null) {
            return;
        }
        if (JbmConstants.ROOT.equals(user.getUserName())) {
            throw new ServiceException("默认用户无需分配!");
        }
        // 先清空,在添加
        removeUserRoles(userId);
        if (roles.length > 0) {
            for (String roleId : roles) {
                BaseRoleUser roleUser = new BaseRoleUser();
                roleUser.setUserId(userId);
                roleUser.setRoleId(Long.parseLong(roleId));
                baseRoleUserMapper.insert(roleUser);
            }
            // 批量保存
        }
    }

    /**
     * 角色添加成员
     *
     * @param roleId
     * @param userIds
     */
    @Override
    public void saveRoleUsers(Long roleId, String... userIds) {
        if (roleId == null || userIds == null) {
            return;
        }
        // 先清空,在添加
        removeRoleUsers(roleId);
        if (userIds.length > 0) {
            for (String userId : userIds) {
                BaseRoleUser roleUser = new BaseRoleUser();
                roleUser.setUserId(Long.parseLong(userId));
                roleUser.setRoleId(roleId);
                baseRoleUserMapper.insert(roleUser);
            }
            // 批量保存
        }
    }

    /**
     * 查询角色成员
     *
     * @return
     */
    @Override
    public List<BaseRoleUser> findRoleUsers(Long roleId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getRoleId, roleId);
        return baseRoleUserMapper.selectList(queryWrapper);
    }

    /**
     * 获取角色所有授权组员数量
     *
     * @param roleId
     * @return
     */
    @Override
    public Long getCountByRole(Long roleId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getRoleId, roleId);
        Long result = baseRoleUserMapper.selectCount(queryWrapper);
        return result;
    }

    /**
     * 获取组员角色数量
     *
     * @param userId
     * @return
     */
    @Override
    public Long getCountByUser(Long userId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getUserId, userId);
        Long result = baseRoleUserMapper.selectCount(queryWrapper);
        return result;
    }

    /**
     * 移除角色所有组员
     *
     * @param roleId
     * @return
     */
    @Override
    public void removeRoleUsers(Long roleId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getRoleId, roleId);
        baseRoleUserMapper.delete(queryWrapper);
    }

    /**
     * 移除组员的所有角色
     *
     * @param userId
     * @return
     */
    @Override
    public void removeUserRoles(Long userId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getUserId, userId);
        if (ObjectUtil.isNotEmpty(LoginHelper.softGetLoginUser()) && !LoginHelper.isAdmin()) {
            // 仅移除当前用户拥有的角色
            Set<Long> roleIds = LoginHelper.getLoginUser().getRoleIds();
            queryWrapper.lambda().in(CollectionUtil.isNotEmpty(roleIds), BaseRoleUser::getRoleId, roleIds);
        }
        baseRoleUserMapper.delete(queryWrapper);
    }

    /**
     * 检测是否存在
     *
     * @param userId
     * @param roleId
     * @return
     */
    @Override
    public Boolean isExist(Long userId, Long roleId) {
        QueryWrapper<BaseRoleUser> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseRoleUser::getRoleId, roleId);
        queryWrapper.lambda().eq(BaseRoleUser::getUserId, userId);
        baseRoleUserMapper.delete(queryWrapper);
        Long result = baseRoleUserMapper.selectCount(queryWrapper);
        return result > 0;
    }


    /**
     * 获取组员角色
     *
     * @param userId
     * @return
     */
    @Override
    public List<BaseRole> getUserRoles(Long userId) {
        List<BaseRole> roles = baseRoleUserMapper.selectRoleUserList(userId);
        return roles;
    }

    /**
     * 获取用户角色ID列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return baseRoleUserMapper.selectRoleUserIdList(userId);
    }


}

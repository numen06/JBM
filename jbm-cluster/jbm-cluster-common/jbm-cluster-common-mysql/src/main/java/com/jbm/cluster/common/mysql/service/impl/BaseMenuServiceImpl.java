package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseMenuService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wesley.zhang
 */
@Slf4j
@Service
public class BaseMenuServiceImpl extends MasterDataServiceImpl<BaseMenu> implements BaseMenuService {
    @Autowired
    private BaseMenuMapper baseMenuMapper;

    //    @Autowired
//    private BaseAuthorityMapper baseAuthorityMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;

    @Autowired
    private BaseActionService baseActionService;

    @Autowired
    private BaseAppService baseAppService;
    @Autowired
    @Lazy
    private BaseMenuService self;

    @Value("${spring.application.name}")
    private String DEFAULT_SERVICE_ID;

    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    @Override
    public DataPaging<BaseMenu> findListPage(BaseMenuForm form) {
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .likeRight(ObjectUtils.isNotEmpty(form.getMenuCode()), BaseMenu::getMenuCode, form.getMenuCode())
                .likeRight(ObjectUtils.isNotEmpty(form.getMenuName()), BaseMenu::getMenuName, form.getMenuName());
        PageForm pageForm = form.getPageForm() != null ? form.getPageForm() : new PageForm();
        return this.selectEntitys(PageParams.from(pageForm), queryWrapper);
    }

    /**
     * 查询平台菜单
     *
     * @param baseMenu
     * @return
     */
    @Override
    public List<BaseMenu> findPlatformList(BaseMenu baseMenu) {
        QueryWrapper<BaseMenu> baseMenuQueryWrapper = new QueryWrapper<>();
        baseMenuQueryWrapper.lambda().like(ObjectUtil.isNotEmpty(baseMenu.getMenuName()), BaseMenu::getMenuName, baseMenu.getMenuName())
                .like(ObjectUtil.isNotEmpty(baseMenu.getMenuCode()), BaseMenu::getMenuCode, baseMenu.getMenuCode())
                .isNull(BaseMenu::getAppId)
                .eq(ObjectUtil.isNotEmpty(baseMenu.getStatus()), BaseMenu::getStatus, baseMenu.getStatus());
        List<BaseMenu> list = baseMenuMapper.selectList(baseMenuQueryWrapper);
        //根据优先级从小到大排序
        list.sort((BaseMenu h1, BaseMenu h2) -> h1.getPriority().compareTo(h2.getPriority()));
        return list;
    }

    /**
     * 查询列表
     *
     * @return
     */
    @Override
    public List<BaseMenu> findAllList(BaseMenu baseMenu) {
        QueryWrapper<BaseMenu> baseMenuQueryWrapper = new QueryWrapper<>();
        baseMenuQueryWrapper.lambda().like(ObjectUtil.isNotEmpty(baseMenu.getMenuName()), BaseMenu::getMenuName, baseMenu.getMenuName())
                .like(ObjectUtil.isNotEmpty(baseMenu.getMenuCode()), BaseMenu::getMenuCode, baseMenu.getMenuCode())
                .and(ObjectUtil.isNotEmpty(baseMenu.getAppId()), baseMenuLambdaQueryWrapper -> baseMenuLambdaQueryWrapper.eq(BaseMenu::getAppId, baseMenu.getAppId()).or().isNull(BaseMenu::getAppId))
                .eq(ObjectUtil.isNotEmpty(baseMenu.getStatus()), BaseMenu::getStatus, baseMenu.getStatus());
        List<BaseMenu> list = baseMenuMapper.selectList(baseMenuQueryWrapper);
        //根据优先级从小到大排序
        list.sort((BaseMenu h1, BaseMenu h2) -> h1.getPriority().compareTo(h2.getPriority()));
        return list;
    }

    /**
     * 根据主键获取菜单
     *
     * @param menuId
     * @return
     */
    @Override
    public BaseMenu getMenu(Long menuId) {
        return baseMenuMapper.selectById(menuId);
    }

    @Override
    public List<BaseMenu> getMenuByAppId(Long appId) {
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseMenu::getAppId, appId);
        return this.selectEntitys(queryWrapper);
    }

    /**
     * 检查菜单编码是否存在
     *
     * @param menuCode
     * @return
     */
    @Override
    public Boolean isExist(String menuCode) {
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper();
        queryWrapper.lambda()
                .eq(BaseMenu::getMenuCode, menuCode);
        Long count = baseMenuMapper.selectCount(queryWrapper);
        return count > 0 ? true : false;
    }

    @Override
    public BaseMenu saveEntity(BaseMenu menu) {
        return persistMenu(menu);
    }

    private BaseMenu persistMenu(BaseMenu menu) {
        //获取当前用户的APPID
//        JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser();
//        menu.setAppId(jbmLoginUser.getAppId());
        if (ObjectUtil.isNotEmpty(menu.getMenuId())) {
            BaseMenu saved = getMenu(menu.getMenuId());
            if (saved == null) {
                throw new ServiceException(String.format("%s信息不存在!", menu.getMenuId()));
            }
            if (!saved.getMenuCode().equals(menu.getMenuCode())) {
                // 和原来不一致重新检查唯一性
                if (isExist(menu.getMenuCode())) {
                    throw new ServiceException(String.format("%s编码已存在!", menu.getMenuCode()));
                }
            }
        } else {
            if (isExist(menu.getMenuCode())) {
                throw new ServiceException(String.format("%s编码已存在!", menu.getMenuCode()));
            }
        }
        if (StrUtil.isEmpty(menu.getScheme())) {
            menu.setScheme("/");
        }
        if (StrUtil.isEmpty(menu.getPath())) {
            menu.setPath("");
        }
        if (StrUtil.isEmpty(menu.getTarget())) {
            menu.setTarget("_self");
        }
        if (ObjectUtil.isEmpty(menu.getStatus())) {
            menu.setStatus(1);
        }
        if (ObjectUtil.isEmpty(menu.getParentId())) {
            menu.setParentId(0L);
        }
        if (ObjectUtil.isEmpty(menu.getPriority())) {
            menu.setPriority(0);
        }
        menu.setServiceId(DEFAULT_SERVICE_ID);
        super.saveEntity(menu);
        if (ObjectUtil.isEmpty(menu.getAppId())) {
            //如果为空则置空
            UpdateWrapper<BaseMenu> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().set(BaseMenu::getAppId, null).eq(BaseMenu::getMenuId, menu.getMenuId());
            this.update(updateWrapper);
        }
        // 同步权限表里的信息
        baseAuthorityService.saveOrUpdateAuthority(menu.getMenuId(), ResourceType.menu);
//        applicationContext.publishEvent(new NewMenuEvent(this, menu));
        return menu;
    }

    @Override
    public boolean deleteEntity(BaseMenu menu) {
        try {
            self.removeMenu(menu.getMenuId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加菜单资源
     *
     * @param menu
     * @return
     */
    @Override
    public BaseMenu addMenu(BaseMenu menu) {
        return persistMenu(menu);
    }

    /**
     * 修改菜单资源
     *
     * @param menu
     * @return
     */
    @Override
    public BaseMenu updateMenu(BaseMenu menu) {
        return persistMenu(menu);
    }


    /**
     * 移除菜单
     *
     * @param menuId
     * @return
     */
    @Override
    public void removeMenu(Long menuId) {
        BaseMenu menu = getMenu(menuId);
        if (BooleanUtil.isTrue(menu.getIsPersist())) {
            throw new ServiceException(String.format("保留数据,不允许删除!"));
        }
        // 移除菜单权限
        baseAuthorityService.removeAuthority(menuId, ResourceType.menu);
        // 移除功能按钮和相关权限
        baseActionService.removeByMenuId(menuId);
        // 移除菜单信息
        baseMenuMapper.deleteById(menuId);
    }

    /**
     * 根据path和appId查询菜单
     *
     * @param path
     * @param appId
     * @return
     */
    @Override
    public BaseMenu getMenuByPathAndAppId(String path, Long appId) {
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseMenu::getPath, path);
        queryWrapper.lambda().eq(ObjectUtil.isNotEmpty(appId), BaseMenu::getAppId, appId);
        List<BaseMenu> list = baseMenuMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }

    /**
     * 根据menuCode查询菜单
     *
     * @param menuCode
     * @return
     */
    @Override
    public BaseMenu getMenuByCode(String menuCode) {
        if (StrUtil.isEmpty(menuCode)) {
            return null;
        }
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseMenu::getMenuCode, menuCode);
        List<BaseMenu> list = baseMenuMapper.selectList(queryWrapper);
        return CollUtil.getFirst(list);
    }

    /**
     * 批量导入菜单
     * 使用menuCode作为判断依据，因为不同系统已经添加了各自的菜单
     *
     * @param menus
     * @return
     */
    @Override
    public int importMenus(List<BaseMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return 0;
        }
        
        int successCount = 0;
        int updateCount = 0;
        int insertCount = 0;
        
        // 先按优先级排序，确保父菜单先处理
        menus.sort((m1, m2) -> {
            int p1 = m1.getPriority() != null ? m1.getPriority() : 0;
            int p2 = m2.getPriority() != null ? m2.getPriority() : 0;
            return Integer.compare(p1, p2);
        });
        
        // 建立旧menuId到menuCode的映射（用于处理parentId）
        // 导入的JSON中parentId是旧的menuId，我们需要找到对应的menuCode
        Map<Long, String> oldMenuIdToCodeMapping = new HashMap<>();
        for (BaseMenu menu : menus) {
            if (menu.getMenuId() != null && StrUtil.isNotEmpty(menu.getMenuCode())) {
                oldMenuIdToCodeMapping.put(menu.getMenuId(), menu.getMenuCode());
            }
        }
        
        // 建立menuCode到新menuId的映射（用于处理parentId）
        Map<String, Long> menuCodeToIdMapping = new HashMap<>();
        // 保存每个导入菜单的最终ID和原始parentId（用于第二遍更新parentId）
        Map<BaseMenu, Long> menuFinalIdMap = new HashMap<>();
        Map<BaseMenu, Long> menuOldParentIdMap = new HashMap<>();
        
        for (BaseMenu importMenu : menus) {
            try {
                // 验证menuCode不能为空
                if (StrUtil.isEmpty(importMenu.getMenuCode())) {
                    log.warn("跳过菜单导入，menuCode为空: {}", importMenu.getMenuName());
                    continue;
                }
                
                // 验证appId是否存在（如果appId不为null）
                if (ObjectUtil.isNotEmpty(importMenu.getAppId())) {
                    com.jbm.cluster.api.entitys.basic.BaseApp app = baseAppService.getAppInfo(importMenu.getAppId());
                    if (app == null) {
                        log.warn("跳过菜单导入，系统中不存在appId: {}, 菜单: {} [menuCode: {}]", 
                            importMenu.getAppId(), importMenu.getMenuName(), importMenu.getMenuCode());
                        continue;
                    }
                }
                
                // 保存原始的parentId（导入JSON中的旧menuId）
                Long oldParentId = importMenu.getParentId();
                
                // 根据menuCode查询是否已存在
                BaseMenu existingMenu = getMenuByCode(importMenu.getMenuCode());
                
                Long finalMenuId;
                if (existingMenu != null) {
                    // 已存在，使用现有的menuId进行更新
                    importMenu.setMenuId(existingMenu.getMenuId());
                    // 临时清空parentId，避免引用不存在的ID，后续会重新设置
                    importMenu.setParentId(null);
                    // 使用saveEntity方法，确保业务逻辑完整执行
                    persistMenu(importMenu);
                    finalMenuId = existingMenu.getMenuId();
                    updateCount++;
                    log.info("更新菜单: {} [menuCode: {}]", importMenu.getMenuName(), importMenu.getMenuCode());
                } else {
                    // 不存在，清空menuId进行新增
                    importMenu.setMenuId(null);
                    // 临时清空parentId，避免引用不存在的ID，后续会重新设置
                    importMenu.setParentId(null);
                    // 使用saveEntity方法，确保业务逻辑完整执行
                    persistMenu(importMenu);
                    finalMenuId = importMenu.getMenuId();
                    insertCount++;
                    log.info("新增菜单: {} [menuCode: {}]", importMenu.getMenuName(), importMenu.getMenuCode());
                }
                
                // 建立menuCode到menuId的映射关系
                menuCodeToIdMapping.put(importMenu.getMenuCode(), finalMenuId);
                
                // 保存菜单的最终ID和原始parentId
                menuFinalIdMap.put(importMenu, finalMenuId);
                if (oldParentId != null && oldParentId > 0) {
                    menuOldParentIdMap.put(importMenu, oldParentId);
                }
                
                successCount++;
            } catch (Exception e) {
                log.error("导入菜单失败: {} [menuCode: {}], 错误: {}", 
                    importMenu.getMenuName(), importMenu.getMenuCode(), e.getMessage(), e);
            }
        }
        
        // 第二遍：更新parentId映射关系（基于menuCode）
        for (Map.Entry<BaseMenu, Long> entry : menuFinalIdMap.entrySet()) {
            BaseMenu importMenu = entry.getKey();
            Long finalMenuId = entry.getValue();
            
            Long oldParentId = menuOldParentIdMap.get(importMenu);
            if (oldParentId != null && oldParentId > 0) {
                // 通过旧parentId找到对应的menuCode
                String parentCode = oldMenuIdToCodeMapping.get(oldParentId);
                if (StrUtil.isNotEmpty(parentCode)) {
                    // 通过parentCode查找对应的新menuId
                    Long newParentId = menuCodeToIdMapping.get(parentCode);
                    if (newParentId != null) {
                        // 需要更新parentId
                        BaseMenu menuToUpdate = getMenu(finalMenuId);
                        if (menuToUpdate != null && !newParentId.equals(menuToUpdate.getParentId())) {
                            menuToUpdate.setParentId(newParentId);
                            persistMenu(menuToUpdate);
                            log.info("更新菜单parentId: {} [menuCode: {}] -> {} [menuCode: {}]", 
                                finalMenuId, importMenu.getMenuCode(), newParentId, parentCode);
                        }
                    } else {
                        log.warn("未找到父菜单menuCode: {}, 菜单: {} [menuCode: {}]", 
                            parentCode, importMenu.getMenuName(), importMenu.getMenuCode());
                    }
                } else {
                    log.warn("未找到父菜单的menuCode，oldParentId: {}, 菜单: {} [menuCode: {}]", 
                        oldParentId, importMenu.getMenuName(), importMenu.getMenuCode());
                }
            }
        }
        
        log.info("菜单导入完成，成功: {}, 新增: {}, 更新: {}", successCount, insertCount, updateCount);
        return successCount;
    }


}

package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.jbm.cluster.api.constants.ResourceType;
import com.jbm.cluster.api.entitys.basic.BaseAction;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.common.mysql.mapper.BaseMenuMapper;
import com.jbm.cluster.common.mysql.service.BaseActionService;
import com.jbm.cluster.common.mysql.service.BaseAppService;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.common.mysql.service.BaseMenuService;
import com.jbm.cluster.common.mysql.service.MenuDataScopeHelper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

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
    private MenuDataScopeHelper menuDataScopeHelper;
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
        BaseMenuForm queryForm = form != null ? form : new BaseMenuForm();
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<BaseMenu> lambda = queryWrapper.lambda();
        if (StrUtil.isNotBlank(queryForm.getKeyword())) {
            String kw = queryForm.getKeyword().trim();
            lambda.and(w -> w.like(BaseMenu::getMenuCode, kw)
                    .or().like(BaseMenu::getMenuName, kw)
                    .or().like(BaseMenu::getPath, kw));
        } else {
            lambda.likeRight(ObjectUtils.isNotEmpty(queryForm.getMenuCode()), BaseMenu::getMenuCode, queryForm.getMenuCode())
                    .likeRight(ObjectUtils.isNotEmpty(queryForm.getMenuName()), BaseMenu::getMenuName, queryForm.getMenuName());
        }
        lambda.likeRight(ObjectUtils.isNotEmpty(queryForm.getPath()), BaseMenu::getPath, queryForm.getPath())
                .eq(ObjectUtils.isNotEmpty(queryForm.getStatus()), BaseMenu::getStatus, queryForm.getStatus());
        applyScopeFilter(lambda, queryForm.getScope(), queryForm.getAppId());
        menuDataScopeHelper.applyToMenuQuery(lambda);
        queryWrapper.orderByAsc("parent_id", "priority", "menu_id");
        PageForm pageForm = queryForm.getPageForm() != null ? queryForm.getPageForm() : new PageForm();
        return this.selectEntitys(PageParams.from(pageForm), queryWrapper);
    }

    private void applyScopeFilter(LambdaQueryWrapper<BaseMenu> lambda, String scope, Long appId) {
        String normalized = StrUtil.blankToDefault(scope, "all");
        switch (normalized) {
            case "platform":
                lambda.isNull(BaseMenu::getAppId);
                break;
            case "app":
                if (ObjectUtil.isNotEmpty(appId)) {
                    lambda.eq(BaseMenu::getAppId, appId);
                } else {
                    lambda.isNotNull(BaseMenu::getAppId);
                }
                break;
            case "visible":
                if (ObjectUtil.isNotEmpty(appId)) {
                    lambda.and(w -> w.eq(BaseMenu::getAppId, appId).or().isNull(BaseMenu::getAppId));
                }
                break;
            default:
                if (ObjectUtil.isNotEmpty(appId)) {
                    lambda.eq(BaseMenu::getAppId, appId);
                }
                break;
        }
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
        list.sort(menuComparator());
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
        list.sort(menuComparator());
        return list;
    }

    private Comparator<BaseMenu> menuComparator() {
        Comparator<Integer> integerComparator = Comparator.nullsLast(Integer::compareTo);
        Comparator<Long> longComparator = Comparator.nullsLast(Long::compareTo);
        return Comparator.comparing(BaseMenu::getParentId, longComparator)
                .thenComparing(BaseMenu::getPriority, integerComparator)
                .thenComparing(BaseMenu::getMenuId, longComparator);
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
    public Boolean isExist(String menuCode, Long appId) {
        return getMenuByCode(menuCode, appId) != null;
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
                if (isExist(menu.getMenuCode(), menu.getAppId())) {
                    throw new ServiceException(String.format("%s编码已存在!", menu.getMenuCode()));
                }
            }
        } else {
            if (isExist(menu.getMenuCode(), menu.getAppId())) {
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
    public BaseMenu getMenuByCode(String menuCode, Long appId) {
        if (StrUtil.isEmpty(menuCode)) {
            return null;
        }
        QueryWrapper<BaseMenu> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<BaseMenu> lambda = queryWrapper.lambda();
        lambda.eq(BaseMenu::getMenuCode, menuCode);
        if (ObjectUtil.isEmpty(appId)) {
            lambda.isNull(BaseMenu::getAppId);
        } else {
            lambda.eq(BaseMenu::getAppId, appId);
        }
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
        int skippedPersistMenuCount = 0;
        int actionSuccessCount = 0;
        int skippedPersistActionCount = 0;

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
                BaseMenu existingMenu = getMenuByCode(importMenu.getMenuCode(), importMenu.getAppId());

                Long finalMenuId;
                if (existingMenu != null) {
                    finalMenuId = existingMenu.getMenuId();
                    if (BooleanUtil.isTrue(existingMenu.getIsPersist())) {
                        skippedPersistMenuCount++;
                        menuCodeToIdMapping.put(importMenu.getMenuCode(), finalMenuId);
                        menuFinalIdMap.put(importMenu, finalMenuId);
                        if (oldParentId != null && oldParentId > 0) {
                            menuOldParentIdMap.put(importMenu, oldParentId);
                        }
                        log.info("跳过保留菜单覆盖: {} [menuCode: {}]", existingMenu.getMenuName(), existingMenu.getMenuCode());
                        continue;
                    }
                    // 已存在且不是保留菜单，使用现有的menuId进行更新
                    importMenu.setMenuId(existingMenu.getMenuId());
                    // 临时清空parentId，避免引用不存在的ID，后续会重新设置
                    importMenu.setParentId(null);
                    // 使用saveEntity方法，确保业务逻辑完整执行
                    persistMenu(importMenu);
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

                int[] actionImportResult = importMenuActions(importMenu.getImportActionList(), finalMenuId);
                actionSuccessCount += actionImportResult[0];
                skippedPersistActionCount += actionImportResult[1];
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
                            if (BooleanUtil.isTrue(menuToUpdate.getIsPersist())) {
                                log.info("跳过保留菜单parentId覆盖: {} [menuCode: {}]", finalMenuId, importMenu.getMenuCode());
                                continue;
                            }
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

        log.info("菜单导入完成，成功: {}, 新增: {}, 更新: {}, 跳过保留菜单: {}, 按钮成功: {}, 跳过保留按钮: {}",
            successCount, insertCount, updateCount, skippedPersistMenuCount, actionSuccessCount, skippedPersistActionCount);
        return successCount;
    }

    private int[] importMenuActions(List<BaseAction> actions, Long menuId) {
        if (CollUtil.isEmpty(actions) || menuId == null) {
            return new int[]{0, 0};
        }
        int successCount = 0;
        int skippedPersistCount = 0;
        for (BaseAction importAction : actions) {
            try {
                if (StrUtil.isEmpty(importAction.getActionCode())) {
                    log.warn("跳过按钮导入，actionCode为空: {}", importAction.getActionName());
                    continue;
                }
                BaseAction existingAction = baseActionService.getActionByMenuIdAndCode(menuId, importAction.getActionCode());
                if (existingAction == null) {
                    existingAction = baseActionService.getActionByCode(importAction.getActionCode());
                }
                if (existingAction != null) {
                    if (existingAction.getIsPersist() != null && existingAction.getIsPersist() == 1) {
                        skippedPersistCount++;
                        log.info("跳过保留按钮覆盖: {} [actionCode: {}]", existingAction.getActionName(), existingAction.getActionCode());
                        continue;
                    }
                    importAction.setActionId(existingAction.getActionId());
                    importAction.setMenuId(menuId);
                    baseActionService.updateAction(importAction);
                    log.info("更新按钮: {} [actionCode: {}]", importAction.getActionName(), importAction.getActionCode());
                } else {
                    importAction.setActionId(null);
                    importAction.setMenuId(menuId);
                    baseActionService.addAction(importAction);
                    log.info("新增按钮: {} [actionCode: {}]", importAction.getActionName(), importAction.getActionCode());
                }
                successCount++;
            } catch (Exception e) {
                log.error("导入按钮失败: {} [actionCode: {}], 错误: {}",
                    importAction.getActionName(), importAction.getActionCode(), e.getMessage(), e);
            }
        }
        return new int[]{successCount, skippedPersistCount};
    }

    @Override
    public int syncMenusFromApp(Long sourceAppId, Long targetAppId, String mode) {
        if (ObjectUtil.isEmpty(sourceAppId) || ObjectUtil.isEmpty(targetAppId)) {
            throw new ServiceException("sourceAppId and targetAppId are required");
        }
        if (sourceAppId.equals(targetAppId)) {
            throw new ServiceException("source and target app must differ");
        }
        if (baseAppService.getAppInfo(sourceAppId) == null) {
            throw new ServiceException("source app not found: " + sourceAppId);
        }
        if (baseAppService.getAppInfo(targetAppId) == null) {
            throw new ServiceException("target app not found: " + targetAppId);
        }
        String syncMode = StrUtil.blankToDefault(mode, "merge").trim().toLowerCase();
        if ("replace".equals(syncMode)) {
            removeNonPersistMenusByAppId(targetAppId);
        }
        List<BaseMenu> sourceMenus = getMenuByAppId(sourceAppId);
        if (CollUtil.isEmpty(sourceMenus)) {
            return 0;
        }
        sortMenusByDepth(sourceMenus);
        Map<Long, Long> sourceToTargetMenuId = new HashMap<>();
        Map<Long, BaseMenu> sourceById = sourceMenus.stream()
                .collect(Collectors.toMap(BaseMenu::getMenuId, menu -> menu, (left, right) -> left));
        int count = 0;
        for (BaseMenu source : sourceMenus) {
            Long targetParentId = resolveSyncedParentId(source, sourceById, sourceToTargetMenuId);
            BaseMenu existing = getMenuByCode(source.getMenuCode(), targetAppId);
            if (existing != null && BooleanUtil.isTrue(existing.getIsPersist())) {
                sourceToTargetMenuId.put(source.getMenuId(), existing.getMenuId());
                count += syncMenuActions(source.getMenuId(), existing.getMenuId());
                continue;
            }
            BaseMenu target = cloneMenuForSync(source, targetAppId, targetParentId);
            BaseMenu saved;
            if (existing != null) {
                target.setMenuId(existing.getMenuId());
                saved = updateMenu(target);
            } else {
                saved = addMenu(target);
            }
            sourceToTargetMenuId.put(source.getMenuId(), saved.getMenuId());
            count++;
            count += syncMenuActions(source.getMenuId(), saved.getMenuId());
        }
        return count;
    }

    private void removeNonPersistMenusByAppId(Long appId) {
        List<BaseMenu> menus = getMenuByAppId(appId);
        if (CollUtil.isEmpty(menus)) {
            return;
        }
        Set<Long> remaining = menus.stream()
                .filter(menu -> !BooleanUtil.isTrue(menu.getIsPersist()))
                .map(BaseMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        if (remaining.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> children = new HashMap<>();
        for (BaseMenu menu : menus) {
            if (!remaining.contains(menu.getMenuId())) {
                continue;
            }
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(menu.getMenuId());
        }
        boolean progressed;
        do {
            progressed = false;
            List<Long> leafMenuIds = remaining.stream()
                    .filter(menuId -> {
                        List<Long> childIds = children.get(menuId);
                        return childIds == null || childIds.stream().noneMatch(remaining::contains);
                    })
                    .collect(Collectors.toList());
            for (Long menuId : leafMenuIds) {
                removeMenu(menuId);
                remaining.remove(menuId);
                progressed = true;
            }
        } while (progressed && !remaining.isEmpty());
    }

    private void sortMenusByDepth(List<BaseMenu> menus) {
        Map<Long, BaseMenu> byId = menus.stream()
                .collect(Collectors.toMap(BaseMenu::getMenuId, menu -> menu, (left, right) -> left));
        menus.sort(Comparator.comparingInt(menu -> menuDepth(menu, byId)));
    }

    private int menuDepth(BaseMenu menu, Map<Long, BaseMenu> byId) {
        if (menu == null || menu.getParentId() == null || menu.getParentId() <= 0) {
            return 0;
        }
        BaseMenu parent = byId.get(menu.getParentId());
        if (parent == null) {
            return 0;
        }
        return 1 + menuDepth(parent, byId);
    }

    private Long resolveSyncedParentId(BaseMenu source, Map<Long, BaseMenu> sourceById, Map<Long, Long> sourceToTargetMenuId) {
        Long parentId = source.getParentId();
        if (parentId == null || parentId <= 0) {
            return 0L;
        }
        Long mappedParentId = sourceToTargetMenuId.get(parentId);
        if (mappedParentId != null) {
            return mappedParentId;
        }
        if (sourceById.containsKey(parentId)) {
            return 0L;
        }
        return 0L;
    }

    private BaseMenu cloneMenuForSync(BaseMenu source, Long targetAppId, Long parentId) {
        BaseMenu target = new BaseMenu();
        target.setMenuCode(source.getMenuCode());
        target.setMenuName(source.getMenuName());
        target.setIcon(source.getIcon());
        target.setParentId(parentId);
        target.setScheme(source.getScheme());
        target.setPath(source.getPath());
        target.setTarget(source.getTarget());
        target.setPriority(source.getPriority());
        target.setMenuDesc(source.getMenuDesc());
        target.setStatus(source.getStatus());
        target.setHidden(source.getHidden());
        target.setServiceId(source.getServiceId());
        target.setAppId(targetAppId);
        target.setIsPersist(false);
        return target;
    }

    private int syncMenuActions(Long sourceMenuId, Long targetMenuId) {
        List<BaseAction> actions = baseActionService.findListByMenuId(sourceMenuId);
        if (CollUtil.isEmpty(actions)) {
            return 0;
        }
        int successCount = 0;
        for (BaseAction sourceAction : actions) {
            if (StrUtil.isEmpty(sourceAction.getActionCode())) {
                continue;
            }
            BaseAction existingAction = baseActionService.getActionByMenuIdAndCode(targetMenuId, sourceAction.getActionCode());
            if (existingAction != null && existingAction.getIsPersist() != null && existingAction.getIsPersist() == 1) {
                continue;
            }
            BaseAction targetAction = new BaseAction();
            targetAction.setActionCode(sourceAction.getActionCode());
            targetAction.setActionName(sourceAction.getActionName());
            targetAction.setMenuId(targetMenuId);
            targetAction.setPriority(sourceAction.getPriority());
            targetAction.setStatus(sourceAction.getStatus());
            targetAction.setIsPersist(0);
            if (existingAction != null) {
                targetAction.setActionId(existingAction.getActionId());
                baseActionService.updateAction(targetAction);
            } else {
                baseActionService.addAction(targetAction);
            }
            successCount++;
        }
        return successCount;
    }

}

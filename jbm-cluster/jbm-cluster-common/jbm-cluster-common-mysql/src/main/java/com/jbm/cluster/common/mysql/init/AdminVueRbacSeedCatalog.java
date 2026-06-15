package com.jbm.cluster.common.mysql.init;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台 Vue 菜单/按钮种子目录，对应 classpath:data/admin-vue-rbac-seed.json。
 */
public class AdminVueRbacSeedCatalog {

    private List<MenuSeed> menus = new ArrayList<>();
    private List<ActionSeed> actions = new ArrayList<>();

    public List<MenuSeed> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuSeed> menus) {
        this.menus = menus != null ? menus : new ArrayList<>();
    }

    public List<ActionSeed> getActions() {
        return actions;
    }

    public void setActions(List<ActionSeed> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public static class MenuSeed {
        private Long id;
        private Long parentId;
        private String code;
        private String name;
        private String path;
        private int priority;
        private boolean grantToSuperAdmin;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isGrantToSuperAdmin() {
            return grantToSuperAdmin;
        }

        public void setGrantToSuperAdmin(boolean grantToSuperAdmin) {
            this.grantToSuperAdmin = grantToSuperAdmin;
        }
    }

    public static class ActionSeed {
        private Long id;
        private Long menuId;
        private String code;
        private String name;
        private int priority;
        private boolean grantToSuperAdmin;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getMenuId() {
            return menuId;
        }

        public void setMenuId(Long menuId) {
            this.menuId = menuId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isGrantToSuperAdmin() {
            return grantToSuperAdmin;
        }

        public void setGrantToSuperAdmin(boolean grantToSuperAdmin) {
            this.grantToSuperAdmin = grantToSuperAdmin;
        }
    }
}

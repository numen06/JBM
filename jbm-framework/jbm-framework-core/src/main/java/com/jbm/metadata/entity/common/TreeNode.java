package com.jbm.metadata.entity.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Numen
 */
@Data
public class TreeNode {
    protected Long id;
    protected Long parentId;
    private String type;
    private String name;
    private Boolean checked;
    private String icon;
    List<TreeNode> children = new ArrayList<>();

    public TreeNode() {
    }

    public TreeNode(Long id, String name, String type, Boolean checked, String icon, Long parentId) {
        this.id = id;
        this.parentId = parentId;
        this.type = type;
        this.name = name;
        this.checked = checked;
        this.icon = icon;
    }

    public void add(TreeNode node) {
        children.add(node);
    }
}

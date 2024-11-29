package com.jbm.util.tree;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;

import java.util.ArrayList;
import java.util.List;

public class TreePathFinder<T> {
    private final Tree<T> tree;

    public TreePathFinder(Tree<T> tree) {
        this.tree = tree;
    }


    public List<Tree<T>> getParents(T id) {
        List<Tree<T>> result = new ArrayList<Tree<T>>();
        Tree<T> node = TreeUtil.getNode(tree, id);
        if (node == null) {
            return null;
        }
        getParent(result, node);
        return result;
    }

    public void getParent(List<Tree<T>> treeList, Tree<T> node) {
        if (node.getParent() != null) {
            treeList.add(node.getParent());
            getParent(treeList, node.getParent());
        }
    }

}
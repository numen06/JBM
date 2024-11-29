package test.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import com.jbm.util.tree.TreePathFinder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@Slf4j
public class TreeTest {
    // 构建node列表
    List<TreeNode<String>> nodeList = CollUtil.newArrayList();

    @Before
    public void before() {
        nodeList.add(new TreeNode<>("1", "0", "系统管理", 5));
        nodeList.add(new TreeNode<>("11", "1", "用户管理", 222222));
        nodeList.add(new TreeNode<>("111", "11", "用户添加", 0));
        nodeList.add(new TreeNode<>("2", "0", "店铺管理", 1));
        nodeList.add(new TreeNode<>("21", "2", "商品管理", 44));
        nodeList.add(new TreeNode<>("221", "2", "商品管理2", 2));
    }


    @Test
    public void test() {
        // 0表示最顶层的id是0
        List<Tree<String>> treeList = TreeUtil.build(nodeList, "0");
//        log.debug("treeList:{}", treeList);
        treeList.forEach(tree -> {
            TreePathFinder<String> treePathFinder = new TreePathFinder<>(tree);
            List<Tree<String>> nodes = treePathFinder.getParents("221");
            System.out.println(nodes);

        });

    }
}

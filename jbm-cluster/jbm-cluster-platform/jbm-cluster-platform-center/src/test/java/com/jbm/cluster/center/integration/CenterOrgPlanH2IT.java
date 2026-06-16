package com.jbm.cluster.center.integration;

import com.jbm.cluster.api.entitys.basic.BaseOrg;
import com.jbm.cluster.api.entitys.basic.BaseUserOrg;
import com.jbm.cluster.center.controller.BaseOrgController;
import com.jbm.cluster.center.controller.BaseUserController;
import com.jbm.cluster.center.integration.support.CenterH2ApiTestSupport;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.metadata.bean.ResultBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 组织/多租户计划 — H2 后端冒烟（Liquibase V13/V14 + 组织 API）。
 */
class CenterOrgPlanH2IT extends CenterH2ApiTestSupport {

    @Autowired
    private BaseOrgController baseOrgController;
    @Autowired
    private BaseUserController baseUserController;

    @Test
    @DisplayName("组织树与默认组织、用户组织授权 API")
    void orgPlan_smoke() {
        ResultBody<List<BaseOrg>> roots = baseOrgController.root(new MasterDataRequsetBody());
        assertSuccess(roots);
        boolean hasDefault = roots.getResult().stream()
                .anyMatch(o -> Long.valueOf(1L).equals(o.getId()) || "默认组织".equals(o.getOrgName()));
        assertThat(hasDefault).as("默认组织 id=1").isTrue();

        BaseOrg child = new BaseOrg();
        child.setOrgName("计划IT子组织");
        child.setParentId(1L);
        MasterDataRequsetBody saveBody = new MasterDataRequsetBody();
        saveBody.put("baseOrg", child);
        ResultBody<BaseOrg> saved = baseOrgController.save(saveBody);
        assertSuccess(saved);
        assertThat(saved.getResult().getId()).isNotNull();
        assertThat(saved.getResult().getOrgName()).isEqualTo("计划IT子组织");

        ResultBody<List<BaseOrg>> tree = baseOrgController.tree(new MasterDataRequsetBody());
        assertSuccess(tree);
        BaseOrg defaultInTree = tree.getResult().stream()
                .filter(o -> Long.valueOf(1L).equals(o.getId()))
                .findFirst()
                .orElse(null);
        assertThat(defaultInTree).isNotNull();
        assertThat(defaultInTree.getChildren()).isNotEmpty();
        assertThat(defaultInTree.getChildren().stream()
                .anyMatch(o -> "计划IT子组织".equals(o.getOrgName()))).isTrue();

        MasterDataRequsetBody subtreeBody = new MasterDataRequsetBody();
        BaseOrg filter = new BaseOrg();
        filter.setId(1L);
        subtreeBody.put("baseOrg", filter);
        ResultBody<List<BaseOrg>> subtree = baseOrgController.tree(subtreeBody);
        assertSuccess(subtree);
        assertThat(subtree.getResult().stream().anyMatch(o -> Long.valueOf(1L).equals(o.getId())))
                .as("子树过滤不应包含根节点自身")
                .isFalse();
        assertThat(subtree.getResult().stream()
                .anyMatch(o -> "计划IT子组织".equals(o.getOrgName()))).isTrue();

        // 三级部门：根 -> 二级 -> 三级
        Long level2Id = saved.getResult().getId();
        BaseOrg level3 = new BaseOrg();
        level3.setOrgName("计划三级部门");
        level3.setParentId(level2Id);
        MasterDataRequsetBody level3Body = new MasterDataRequsetBody();
        level3Body.put("baseOrg", level3);
        ResultBody<BaseOrg> savedLevel3 = baseOrgController.save(level3Body);
        assertSuccess(savedLevel3);
        assertThat(savedLevel3.getResult().getParentId()).isEqualTo(level2Id);

        ResultBody<List<BaseOrg>> subtreeAfterL3 = baseOrgController.tree(subtreeBody);
        assertSuccess(subtreeAfterL3);
        BaseOrg level2InTree = findOrgByName(subtreeAfterL3.getResult(), "计划IT子组织");
        assertThat(level2InTree).isNotNull();
        assertThat(level2InTree.getChildren()).isNotEmpty();
        assertThat(level2InTree.getChildren().stream()
                .anyMatch(o -> "计划三级部门".equals(o.getOrgName()))).isTrue();

        ResultBody<List<BaseUserOrg>> orgs = baseUserController.getUserOrgs(1L);
        assertSuccess(orgs);
        assertThat(orgs.getResult()).isNotNull();
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess()).isTrue();
    }

    private static BaseOrg findOrgByName(List<BaseOrg> nodes, String name) {
        if (nodes == null) {
            return null;
        }
        for (BaseOrg node : nodes) {
            if (name.equals(node.getOrgName())) {
                return node;
            }
            BaseOrg found = findOrgByName(node.getChildren(), name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}

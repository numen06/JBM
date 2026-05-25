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

        ResultBody<List<BaseUserOrg>> orgs = baseUserController.getUserOrgs(1L);
        assertSuccess(orgs);
        assertThat(orgs.getResult()).isNotNull();
    }

    private static void assertSuccess(ResultBody<?> body) {
        assertThat(body).isNotNull();
        assertThat(body.getSuccess()).isTrue();
    }
}

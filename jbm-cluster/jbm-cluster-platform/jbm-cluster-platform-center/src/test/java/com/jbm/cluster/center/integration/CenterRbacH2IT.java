package com.jbm.cluster.center.integration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.auth.AuthorityMenu;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.center.JbmCenterApplication;
import com.jbm.cluster.center.integration.support.CenterH2TestConfiguration;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import com.jbm.cluster.common.mysql.mapper.BaseUserMapper;
import com.jbm.cluster.common.mysql.service.BaseAuthorityService;
import com.jbm.cluster.core.constant.JbmConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 空库：Liquibase + SystemDataInitializer 后，超管与菜单权限可用。
 * <pre>
 * mvn test -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am -Dtest=CenterRbacH2IT
 * </pre>
 */
@SpringBootTest(
        classes = JbmCenterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.autoconfigure.exclude=com.alibaba.cloud.nacos.discovery.NacosDiscoveryAutoConfiguration,com.alibaba.cloud.nacos.NacosConfigAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,jbm.framework.boot.autoconfigure.redis.RedisAutoConfiguration",
                "management.endpoints.enabled-by-default=false",
                "jbm.cluster.api-register=false"
        }
)
@ActiveProfiles("h2")
@Import(CenterH2TestConfiguration.class)
class CenterRbacH2IT {

    @MockBean
    private RedisService redisService;

    @Autowired
    private BaseUserMapper baseUserMapper;
    @Autowired
    private BaseAuthorityService baseAuthorityService;

    @Test
    void emptyDatabase_hasRootAdminAndMenus() {
        QueryWrapper<BaseUser> q = new QueryWrapper<>();
        q.lambda().eq(BaseUser::getUserName, JbmConstants.ROOT_USER_NAME);
        BaseUser admin = baseUserMapper.selectOne(q);
        assertThat(admin).isNotNull();
        assertThat(admin.getUserId()).isEqualTo(JbmConstants.ROOT_USER_ID);

        List<OpenAuthority> authorities = baseAuthorityService.findAuthorityByUser(
                JbmConstants.ROOT_USER_ID, true);
        assertThat(authorities).isNotEmpty();

        List<AuthorityMenu> menus = baseAuthorityService.findAuthorityMenuByUser(
                JbmConstants.ROOT_USER_ID, null, true);
        assertThat(menus).isNotEmpty();
    }
}

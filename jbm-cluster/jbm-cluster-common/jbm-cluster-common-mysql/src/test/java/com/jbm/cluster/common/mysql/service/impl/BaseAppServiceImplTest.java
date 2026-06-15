package com.jbm.cluster.common.mysql.service.impl;

import com.jbm.cluster.api.entitys.basic.BaseApp;
import com.jbm.cluster.common.mysql.mapper.BaseAppMapper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseAppServiceImplTest {

    @Mock
    private BaseAppMapper baseAppMapper;

    @Test
    void addAppInfo_defaultsBlankAppTypeToPcBeforeInsert() {
        BaseAppServiceImpl service = new BaseAppServiceImpl();
        ReflectionTestUtils.setField(service, "baseAppMapper", baseAppMapper);
        when(baseAppMapper.insert(any(BaseApp.class))).thenReturn(1);

        BaseApp app = new BaseApp();
        app.setAppName("测试应用");

        service.addAppInfo(app);

        ArgumentCaptor<BaseApp> captor = ArgumentCaptor.forClass(BaseApp.class);
        verify(baseAppMapper).insert(captor.capture());
        BaseApp inserted = captor.getValue();
        assertThat(inserted.getAppType()).isEqualTo("pc");
        assertThat(inserted.getApiKey()).isNotBlank();
        assertThat(inserted.getSecretKey()).isNotBlank();
    }

    @Test
    void restSecretStoresBcryptAndReturnsPlainSecretOnce() {
        BaseAppServiceImpl service = new BaseAppServiceImpl();
        ReflectionTestUtils.setField(service, "baseAppMapper", baseAppMapper);

        BaseApp app = new BaseApp();
        app.setAppId(1000L);
        app.setIsPersist(0);
        when(baseAppMapper.selectById(1000L)).thenReturn(app);
        when(baseAppMapper.updateById(any(BaseApp.class))).thenReturn(1);

        String plainSecret = service.restSecret(1000L);

        ArgumentCaptor<BaseApp> captor = ArgumentCaptor.forClass(BaseApp.class);
        verify(baseAppMapper).updateById(captor.capture());
        String storedSecret = captor.getValue().getSecretKey();
        assertThat(plainSecret).isNotBlank();
        assertThat(storedSecret).startsWith("$2a$");
        assertThat(storedSecret).isNotEqualTo(plainSecret);
        assertThat(SecurityUtils.matchesPassword(plainSecret, storedSecret)).isTrue();
    }
}

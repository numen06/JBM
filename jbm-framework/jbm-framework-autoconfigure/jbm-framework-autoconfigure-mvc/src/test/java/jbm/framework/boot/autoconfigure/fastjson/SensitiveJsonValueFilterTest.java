package jbm.framework.boot.autoconfigure.fastjson;

import com.jbm.util.sensitive.SensitiveContext;
import com.jbm.util.sensitive.SensitiveField;
import com.jbm.util.sensitive.SensitiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveJsonValueFilterTest {

    private final SensitiveJsonValueFilter filter = new SensitiveJsonValueFilter();

    @AfterEach
    void clearContext() {
        SensitiveContext.clear();
    }

    @Test
    void skipsOnlyConfiguredSensitiveType() {
        SensitiveUser user = new SensitiveUser();
        SensitiveContext.skipMask(SensitiveType.NAME);

        assertEquals("张三", filter.process(user, "realName", "张三"));
        assertEquals("151****2213", filter.process(user, "mobile", "15112342213"));
    }

    private static class SensitiveUser {

        @SensitiveField(SensitiveType.NAME)
        private String realName;

        @SensitiveField(SensitiveType.MOBILE)
        private String mobile;
    }
}

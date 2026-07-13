package test.sensitive;

import com.jbm.util.sensitive.SensitiveLogUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SensitiveLogUtilsTest {

    @Test
    public void masksStructuredJsonCredentialsRecursively() {
        String source = "{\"Authorization\":\"Bearer header-secret\","
                + "\"Cookie\":\"sa-token=cookie-secret\","
                + "\"data\":{\"access_token\":\"access-secret\",\"token_type\":\"bearer\"},"
                + "\"items\":[{\"clientSecret\":\"client-secret\"}],\"name\":\"visible\"}";

        String masked = SensitiveLogUtils.maskTokens(source);

        assertFalse(masked.contains("header-secret"));
        assertFalse(masked.contains("cookie-secret"));
        assertFalse(masked.contains("access-secret"));
        assertFalse(masked.contains("client-secret"));
        assertTrue(masked.contains("[REDACTED]"));
        assertTrue(masked.contains("\"token_type\":\"bearer\""));
        assertTrue(masked.contains("\"name\":\"visible\""));
    }

    @Test
    public void masksFormHeadersAndStandaloneJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";
        String source = "Authorization: Basic Y2xpZW50OnNlY3JldA==\n"
                + "path=/oauth&refresh_token=refresh-secret&name=visible\n"
                + "failure token " + jwt;

        String masked = SensitiveLogUtils.maskTokens(source);

        assertFalse(masked.contains("Y2xpZW50OnNlY3JldA=="));
        assertFalse(masked.contains("refresh-secret"));
        assertFalse(masked.contains(jwt));
        assertTrue(masked.contains("name=visible"));
    }

    @Test
    public void masksJsonStoredInsideAStringValue() {
        String source = "{\"response\":\"{\\\"token\\\":\\\"nested-secret\\\",\\\"code\\\":0}\"}";

        String masked = SensitiveLogUtils.maskTokens(source);

        assertFalse(masked.contains("nested-secret"));
        assertTrue(masked.contains("[REDACTED]"));
        assertTrue(masked.contains("code"));
    }
}

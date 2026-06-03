package com.jbm.cluster.common.mysql.service.openapi;

import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSpecSanitizerTest {

    private final OpenApiSpecSanitizer sanitizer = new OpenApiSpecSanitizer();

    @Test
    void filterPublishable_requiresOpenLinkedAndEnabled() {
        OpenApiOperation allowed = op(1L, 1, 1, "ACTIVE");
        OpenApiOperation internal = op(2L, 0, 1, "ACTIVE");
        OpenApiOperation disabled = op(3L, 1, 0, "ACTIVE");
        OpenApiOperation unlinked = op(null, 1, 1, "ACTIVE");
        OpenApiOperation missing = op(4L, 1, 1, "MISSING");

        List<OpenApiOperation> result = sanitizer.filterPublishable(Arrays.asList(
                allowed, internal, disabled, unlinked, missing));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApiId()).isEqualTo(1L);
    }

    @Test
    void buildPublishedSpec_containsPaths() {
        OpenApiOperation op = op(1L, 1, 1, "ACTIVE");
        op.setServiceId("jbm-cluster-platform-center");
        op.setPath("/user/list");
        op.setRequestMethod("GET");
        op.setSummary("用户列表");
        op.setResponsesJson("{\"200\":{\"description\":\"OK\"}}");

        String spec = sanitizer.buildPublishedSpec("Center API", "1.0.0", Arrays.asList(op));

        assertThat(spec).contains("\"openapi\"");
        assertThat(spec).contains("/user/list");
        assertThat(spec).contains("用户列表");
    }

    private static OpenApiOperation op(Long apiId, int isOpen, int status, String syncState) {
        OpenApiOperation op = new OpenApiOperation();
        op.setApiId(apiId);
        op.setIsOpen(isOpen);
        op.setStatus(status);
        op.setSyncState(syncState);
        return op;
    }
}

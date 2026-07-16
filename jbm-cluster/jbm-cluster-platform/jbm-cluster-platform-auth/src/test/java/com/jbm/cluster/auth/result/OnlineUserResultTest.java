package com.jbm.cluster.auth.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OnlineUserResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldNotExposeTokenId() {
        SysUserOnline onlineUser = new SysUserOnline();
        onlineUser.setTokenId("sensitive-token");
        onlineUser.setUserName("tester");

        JsonNode response = objectMapper.valueToTree(OnlineUserResult.from(onlineUser));

        assertFalse(response.has("tokenId"));
        assertEquals("tester", response.get("userName").asText());
    }
}

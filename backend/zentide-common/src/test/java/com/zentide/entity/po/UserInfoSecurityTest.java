package com.zentide.entity.po;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passwordAndApiKeyAreNeverSerialized() throws Exception {
        UserInfo user = new UserInfo();
        user.setUserId("1000000001");
        user.setNickName("社区成员");
        user.setPassword("password-hash-must-not-leak");
        user.setApiKey("api-key-must-not-leak");

        String json = objectMapper.writeValueAsString(user);

        assertThat(json)
                .contains("1000000001", "社区成员")
                .doesNotContain("password", "apiKey", "password-hash-must-not-leak", "api-key-must-not-leak");
    }
}

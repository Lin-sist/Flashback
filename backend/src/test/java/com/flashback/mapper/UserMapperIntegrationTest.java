package com.flashback.mapper;

import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserMapperIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldInsertAndQueryUserWithOpenid() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 8, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 10, 8, 5, 0);

        User user = new User();
        user.setUsername("mapper_user_01");
        user.setPasswordHash("hash_01");
        user.setNickname("MapperUser");
        user.setEmail("mapper01@test.com");
        user.setAvatar("https://img.test/avatar-1.png");
        user.setOpenid("openid_abc_123");
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        int inserted = userMapper.insert(user);

        assertThat(inserted).isEqualTo(1);
        assertThat(user.getId()).isNotNull();

        User queriedByUsername = userMapper.selectByUsername("mapper_user_01");
        assertThat(queriedByUsername).isNotNull();
        assertThat(queriedByUsername.getOpenid()).isEqualTo("openid_abc_123");
        assertThat(queriedByUsername.getPasswordHash()).isEqualTo("hash_01");
        assertThat(queriedByUsername.getNickname()).isEqualTo("MapperUser");
        assertThat(queriedByUsername.getStatus()).isEqualTo(UserStatus.ENABLED);
        assertThat(queriedByUsername.getCreatedAt()).isEqualTo(createdAt);
        assertThat(queriedByUsername.getUpdatedAt()).isEqualTo(updatedAt);

        User queriedById = userMapper.selectById(user.getId());
        assertThat(queriedById).isNotNull();
        assertThat(queriedById.getUsername()).isEqualTo("mapper_user_01");
        assertThat(queriedById.getOpenid()).isEqualTo("openid_abc_123");
    }
}

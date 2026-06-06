package com.flashback.mapper;

import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldBindAndQueryUserByOpenid() {
        User user = newUser("mapper_bind_01", null);
        userMapper.insert(user);

        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 11, 9, 0, 0);
        int updated = userMapper.updateOpenidById(user.getId(), "openid_bind_001", updatedAt);

        assertThat(updated).isEqualTo(1);
        User queried = userMapper.selectByOpenid("openid_bind_001");
        assertThat(queried).isNotNull();
        assertThat(queried.getId()).isEqualTo(user.getId());
        assertThat(queried.getOpenid()).isEqualTo("openid_bind_001");
        assertThat(queried.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldRejectDuplicatedOpenid() {
        User first = newUser("mapper_dup_openid_01", "openid_unique_001");
        User second = newUser("mapper_dup_openid_02", null);
        userMapper.insert(first);
        userMapper.insert(second);

        assertThatThrownBy(() -> userMapper.updateOpenidById(
                second.getId(),
                "openid_unique_001",
                LocalDateTime.of(2026, 1, 11, 10, 0, 0)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private User newUser(String username, String openid) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 8, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 10, 8, 5, 0);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash_" + username);
        user.setNickname(username);
        user.setEmail(username + "@test.com");
        user.setAvatar("https://img.test/" + username + ".png");
        user.setOpenid(openid);
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }
}

package com.flashback.mapper;

import com.flashback.domain.Tag;
import com.flashback.domain.TagStatus;
import com.flashback.domain.TagType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagMapperIntegrationTest {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectEnabledByTypeShouldReturnSharedEnabledTagsInStableIdOrder() {
        insertTag(501L, "关闭标签", TagType.MOOD, TagStatus.DISABLED);
        insertTag(503L, "期待", TagType.MOOD, TagStatus.ENABLED);
        insertTag(502L, "工作", TagType.TOPIC, TagStatus.ENABLED);
        insertTag(504L, "焦虑", TagType.MOOD, TagStatus.ENABLED);

        List<Tag> allEnabled = tagMapper.selectEnabledByType(null);
        List<Tag> moodTags = tagMapper.selectEnabledByType(TagType.MOOD);

        assertThat(allEnabled)
                .extracting(Tag::getId)
                .containsExactly(502L, 503L, 504L);
        assertThat(moodTags)
                .extracting(Tag::getId)
                .containsExactly(503L, 504L);
        assertThat(moodTags)
                .extracting(Tag::getStatus)
                .containsOnly(TagStatus.ENABLED);
    }

    @Test
    void countEnabledByIdsShouldIgnoreDisabledTags() {
        insertTag(601L, "已启用", TagType.TOPIC, TagStatus.ENABLED);
        insertTag(602L, "已关闭", TagType.TOPIC, TagStatus.DISABLED);

        long count = tagMapper.countEnabledByIds(List.of(601L, 602L));

        assertThat(count).isEqualTo(1L);
    }

    private void insertTag(Long id, String name, TagType type, TagStatus status) {
        jdbcTemplate.update("""
                INSERT INTO tag (id, name, type, status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                name,
                type.name(),
                status.name(),
                LocalDateTime.of(2026, 4, 10, 10, 0, 0));
    }
}

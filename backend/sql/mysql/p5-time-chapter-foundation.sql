-- P5.x Time Chapter Foundation
-- Run only after explicit Gate 3 authorization.

SET @p5_record_owner_key_sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'record' AND INDEX_NAME = 'uk_record_id_user_id') = 0,
  'ALTER TABLE `record` ADD UNIQUE KEY `uk_record_id_user_id` (`id`, `user_id`)',
  'SELECT 1'
);
PREPARE p5_record_owner_key_stmt FROM @p5_record_owner_key_sql;
EXECUTE p5_record_owner_key_stmt;
DEALLOCATE PREPARE p5_record_owner_key_stmt;

CREATE TABLE IF NOT EXISTS `time_chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `note` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `ended_at` DATETIME DEFAULT NULL,
  `version` BIGINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_time_chapter_id_user_id` (`id`, `user_id`),
  KEY `idx_time_chapter_user_status_updated` (`user_id`, `status`, `updated_at`, `id`),
  KEY `idx_time_chapter_user_created` (`user_id`, `created_at`, `id`),
  CONSTRAINT `ck_time_chapter_status_ended_at`
    CHECK ((`status` = 'ACTIVE' AND `ended_at` IS NULL) OR (`status` = 'ENDED' AND `ended_at` IS NOT NULL)),
  CONSTRAINT `fk_time_chapter_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `time_chapter_record` (
  `chapter_id` BIGINT NOT NULL,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `added_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`chapter_id`, `record_id`),
  UNIQUE KEY `uk_time_chapter_record_record_id` (`record_id`),
  KEY `idx_time_chapter_record_user_chapter_added` (`user_id`, `chapter_id`, `added_at`, `record_id`),
  CONSTRAINT `fk_time_chapter_record_chapter_owner`
    FOREIGN KEY (`chapter_id`, `user_id`) REFERENCES `time_chapter` (`id`, `user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_time_chapter_record_record_owner`
    FOREIGN KEY (`record_id`, `user_id`) REFERENCES `record` (`id`, `user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

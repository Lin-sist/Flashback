CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `openid` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `record_type` VARCHAR(30) NOT NULL DEFAULT 'NODE_RECORD',
  `core_question` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `unlock_at` DATETIME DEFAULT NULL,
  `sealed_at` DATETIME DEFAULT NULL,
  `unlocked_at` DATETIME DEFAULT NULL,
  `ai_summary` TEXT DEFAULT NULL,
  `ai_prompt_result` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_user_id` (`user_id`),
  KEY `idx_record_status` (`status`),
  KEY `idx_record_unlock_at` (`unlock_at`),
  KEY `idx_record_user_status_created` (`user_id`, `status`, `created_at`),
  KEY `idx_record_status_unlock_at` (`status`, `unlock_at`),
  CONSTRAINT `fk_record_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name_type` (`name`, `type`),
  KEY `idx_tag_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `record_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_tag` (`record_id`, `tag_id`),
  KEY `idx_record_tag_record_id` (`record_id`),
  KEY `idx_record_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_record_tag_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_tag_tag_id`
    FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `reply_type` VARCHAR(20) NOT NULL DEFAULT 'SHORT_REPLY',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reply_record_id` (`record_id`),
  KEY `idx_reply_user_id` (`user_id`),
  CONSTRAINT `fk_reply_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reply_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `unlock_notice_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `notice_type` VARCHAR(30) NOT NULL DEFAULT 'SYSTEM_UNLOCK',
  `notice_status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_unlock_notice_record_id` (`record_id`),
  KEY `idx_unlock_notice_user_id` (`user_id`),
  CONSTRAINT `fk_unlock_notice_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_unlock_notice_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

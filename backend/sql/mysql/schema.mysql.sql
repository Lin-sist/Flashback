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
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `record_type` VARCHAR(30) NOT NULL DEFAULT 'MOMENT',
  `core_question` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  `unlock_at` DATETIME DEFAULT NULL,
  `sealed_at` DATETIME DEFAULT NULL,
  `unlocked_at` DATETIME DEFAULT NULL,
  `draft_expires_at` DATETIME DEFAULT NULL,
  `ai_summary` TEXT DEFAULT NULL,
  `ai_prompt_result` TEXT DEFAULT NULL,
  `belief_then` TEXT DEFAULT NULL,
  `reality_later` TEXT DEFAULT NULL,
  `reality_later_submit_count` INT NOT NULL DEFAULT 0,
  `life_node_type` VARCHAR(30) DEFAULT NULL,
  `life_node_custom_label` VARCHAR(50) DEFAULT NULL,
  `cover_attachment_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_user_id` (`user_id`),
  KEY `idx_record_user_created_id` (`user_id`, `created_at`, `id`),
  KEY `idx_record_status` (`status`),
  KEY `idx_record_unlock_at` (`unlock_at`),
  KEY `idx_record_user_status_created` (`user_id`, `status`, `created_at`),
  KEY `idx_record_status_unlock_at` (`status`, `unlock_at`),
  KEY `idx_record_status_draft_expires` (`status`, `draft_expires_at`),
  KEY `idx_record_cover_attachment_id` (`cover_attachment_id`),
  CONSTRAINT `fk_record_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `record_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `source` VARCHAR(30) NOT NULL,
  `name` VARCHAR(100) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_location_record` (`record_id`),
  KEY `idx_record_location_user_id` (`user_id`),
  CONSTRAINT `fk_record_location_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_location_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `record_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `storage_provider` VARCHAR(20) NOT NULL,
  `bucket` VARCHAR(100) NOT NULL,
  `storage_key` VARCHAR(512) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `mime_type` VARCHAR(100) NOT NULL,
  `size_bytes` BIGINT NOT NULL,
  `duration_seconds` INT DEFAULT NULL,
  `width` INT DEFAULT NULL,
  `height` INT DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_attachment_storage_key` (`storage_provider`, `bucket`, `storage_key`),
  KEY `idx_record_attachment_record_status` (`record_id`, `status`, `sort_order`),
  KEY `idx_record_attachment_user_id` (`user_id`),
  CONSTRAINT `fk_record_attachment_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_attachment_user_id`
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

CREATE TABLE `record_reminder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `template_type` VARCHAR(40) NOT NULL,
  `reminder_status` VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
  `last_error` VARCHAR(255) DEFAULT NULL,
  `sent_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_reminder_record_template` (`record_id`, `template_type`),
  KEY `idx_record_reminder_user_status` (`user_id`, `reminder_status`),
  CONSTRAINT `fk_record_reminder_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_reminder_user_id`
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

-- C1 agent-runtime-mvp：Agent 会话与消息。
-- 隐私：agent_message.content 为高敏业务数据，禁止进入应用日志 / telemetry / tracked files。
CREATE TABLE `agent_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `purpose` VARCHAR(30) NOT NULL DEFAULT 'WRITING_GUIDANCE',
  `conversation_intent` VARCHAR(24) DEFAULT NULL,
  `stage` VARCHAR(30) NOT NULL DEFAULT 'WITNESS',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `turn_count` INT NOT NULL DEFAULT 0,
  `stage_reask_count` INT NOT NULL DEFAULT 0,
  `last_active_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_session_user_status` (`user_id`, `status`),
  KEY `idx_agent_session_record_status` (`record_id`, `status`),
  CONSTRAINT `fk_agent_session_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_session_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `turn_no` INT NOT NULL,
  `stage` VARCHAR(30) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_message_session_turn_role` (`session_id`, `turn_no`, `role`),
  KEY `idx_agent_message_session_id` (`session_id`, `id`),
  KEY `idx_agent_message_user_id` (`user_id`),
  CONSTRAINT `fk_agent_message_session_id`
    FOREIGN KEY (`session_id`) REFERENCES `agent_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_message_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `data_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `operation_type` VARCHAR(30) NOT NULL,
  `status` VARCHAR(30) NOT NULL,
  `active_slot` TINYINT GENERATED ALWAYS AS (
    CASE WHEN `status` IN ('PREPARED','PENDING','RUNNING','RETRY_REQUIRED') THEN 1 ELSE NULL END
  ) STORED,
  `sealed_content_policy` VARCHAR(30) DEFAULT NULL,
  `total_items` INT NOT NULL DEFAULT 0,
  `processed_items` INT NOT NULL DEFAULT 0,
  `failed_items` INT NOT NULL DEFAULT 0,
  `confirmation_nonce_hash` CHAR(64) DEFAULT NULL,
  `confirmation_expires_at` DATETIME DEFAULT NULL,
  `artifact_token` CHAR(36) DEFAULT NULL,
  `artifact_expires_at` DATETIME DEFAULT NULL,
  `failure_code` VARCHAR(50) DEFAULT NULL,
  `confirmed_at` DATETIME DEFAULT NULL,
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_operation_artifact_token` (`artifact_token`),
  UNIQUE KEY `uk_data_operation_user_active` (`user_id`, `active_slot`),
  KEY `idx_data_operation_user_status` (`user_id`, `status`, `operation_type`, `id`),
  CONSTRAINT `fk_data_operation_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `data_operation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operation_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `item_status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  `attempt_count` INT NOT NULL DEFAULT 0,
  `failure_code` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_operation_record_target` (`operation_id`, `record_id`),
  KEY `idx_data_operation_record_operation_status` (`operation_id`, `item_status`, `id`),
  KEY `idx_data_operation_record_user_record` (`user_id`, `record_id`, `item_status`),
  CONSTRAINT `fk_data_operation_record_operation_id`
    FOREIGN KEY (`operation_id`) REFERENCES `data_operation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_data_operation_record_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_data_operation_record_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

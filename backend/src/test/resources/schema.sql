DROP TABLE IF EXISTS `agent_message`;
DROP TABLE IF EXISTS `agent_session`;
DROP TABLE IF EXISTS `unlock_notice_log`;
DROP TABLE IF EXISTS `record_reminder`;
DROP TABLE IF EXISTS `reply`;
DROP TABLE IF EXISTS `record_tag`;
DROP TABLE IF EXISTS `record_attachment`;
DROP TABLE IF EXISTS `record_location`;
DROP TABLE IF EXISTS `tag`;
DROP TABLE IF EXISTS `record`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `openid` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_openid` (`openid`)
);

CREATE TABLE `record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) DEFAULT NULL,
  `content` TEXT NOT NULL,
  `record_type` VARCHAR(30) NOT NULL,
  `core_question` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL,
  `unlock_at` DATETIME DEFAULT NULL,
  `sealed_at` DATETIME DEFAULT NULL,
  `unlocked_at` DATETIME DEFAULT NULL,
  `ai_summary` TEXT DEFAULT NULL,
  `ai_prompt_result` TEXT DEFAULT NULL,
  `belief_then` TEXT DEFAULT NULL,
  `reality_later` TEXT DEFAULT NULL,
  `reality_later_submit_count` INT NOT NULL DEFAULT 0,
  `life_node_type` VARCHAR(30) DEFAULT NULL,
  `life_node_custom_label` VARCHAR(50) DEFAULT NULL,
  `cover_attachment_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_record_user_id` (`user_id`),
  KEY `idx_record_user_created_id` (`user_id`, `created_at`, `id`),
  KEY `idx_record_status` (`status`),
  KEY `idx_record_unlock_at` (`unlock_at`),
  KEY `idx_record_user_status_created` (`user_id`, `status`, `created_at`),
  KEY `idx_record_status_unlock_at` (`status`, `unlock_at`),
  KEY `idx_record_cover_attachment_id` (`cover_attachment_id`),
  CONSTRAINT `fk_record_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `record_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `source` VARCHAR(30) NOT NULL,
  `name` VARCHAR(100) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_location_record` (`record_id`),
  KEY `idx_record_location_user_id` (`user_id`),
  CONSTRAINT `fk_record_location_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_location_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

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
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_attachment_storage_key` (`storage_provider`, `bucket`, `storage_key`),
  KEY `idx_record_attachment_record_status` (`record_id`, `status`, `sort_order`),
  KEY `idx_record_attachment_user_id` (`user_id`),
  CONSTRAINT `fk_record_attachment_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_attachment_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name_type` (`name`, `type`),
  KEY `idx_tag_type` (`type`)
);

CREATE TABLE `record_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_tag` (`record_id`, `tag_id`),
  KEY `idx_record_tag_record_id` (`record_id`),
  KEY `idx_record_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_record_tag_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_tag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE RESTRICT
);

CREATE TABLE `unlock_notice_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `notice_type` VARCHAR(30) NOT NULL,
  `notice_status` VARCHAR(20) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_unlock_notice_record_id` (`record_id`),
  KEY `idx_unlock_notice_user_id` (`user_id`),
  CONSTRAINT `fk_unlock_notice_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_unlock_notice_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `record_reminder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `template_type` VARCHAR(40) NOT NULL,
  `reminder_status` VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
  `last_error` VARCHAR(255) DEFAULT NULL,
  `sent_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_reminder_record_template` (`record_id`, `template_type`),
  KEY `idx_record_reminder_user_status` (`user_id`, `reminder_status`),
  CONSTRAINT `fk_record_reminder_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_reminder_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `reply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` VARCHAR(500) NOT NULL,
  `reply_type` VARCHAR(20) NOT NULL DEFAULT 'SHORT_REPLY',
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reply_record_id` (`record_id`),
  KEY `idx_reply_user_id` (`user_id`),
  CONSTRAINT `fk_reply_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reply_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

CREATE TABLE `agent_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `stage` VARCHAR(30) NOT NULL DEFAULT 'OPENING',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `turn_count` INT NOT NULL DEFAULT 0,
  `stage_reask_count` INT NOT NULL DEFAULT 0,
  `last_active_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_agent_session_user_status` (`user_id`, `status`),
  KEY `idx_agent_session_record_status` (`record_id`, `status`),
  CONSTRAINT `fk_agent_session_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_session_record_id` FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE
);

CREATE TABLE `agent_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `turn_no` INT NOT NULL,
  `stage` VARCHAR(30) NOT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_message_session_turn_role` (`session_id`, `turn_no`, `role`),
  KEY `idx_agent_message_session_id` (`session_id`, `id`),
  KEY `idx_agent_message_user_id` (`user_id`),
  CONSTRAINT `fk_agent_message_session_id` FOREIGN KEY (`session_id`) REFERENCES `agent_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_message_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

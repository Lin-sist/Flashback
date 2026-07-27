-- C1 agent-runtime-mvp：Agent 会话与消息持久化。
-- 决策依据：openspec/changes/agent-runtime-mvp/design.md 决策 1（选 MySQL 而非 Redis / 内存）。
-- 可重复执行：仅在表不存在时创建。
-- 隐私：agent_message.content 为高敏业务数据，只允许存在于本表，禁止进入应用日志 / telemetry / tracked files。

CREATE TABLE IF NOT EXISTS `agent_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `stage` VARCHAR(30) NOT NULL DEFAULT 'OPENING',
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

CREATE TABLE IF NOT EXISTS `agent_message` (
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

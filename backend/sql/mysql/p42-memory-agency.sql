-- P4.2 memory-agency
-- 只迁移结构化授权、记录同意元数据与来源关系；不读取或输出日记、消息、note、prompt 或模型文本。

-- Read-only preflight: schema + structured aggregate only.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'agent_session' AND COLUMN_NAME = 'cross_record_memory_enabled')
    OR (TABLE_NAME = 'record' AND COLUMN_NAME IN ('agent_memory_excluded', 'agent_memory_context_note'))
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SET @p42_has_session_auth := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agent_session'
    AND COLUMN_NAME = 'cross_record_memory_enabled'
);
SET @p42_add_session_auth_sql := IF(
  @p42_has_session_auth = 0,
  'ALTER TABLE `agent_session` ADD COLUMN `cross_record_memory_enabled` TINYINT(1) NOT NULL DEFAULT 0 AFTER `stage_reask_count`',
  'SELECT ''agent_session.cross_record_memory_enabled exists'''
);
PREPARE p42_add_session_auth FROM @p42_add_session_auth_sql;
EXECUTE p42_add_session_auth;
DEALLOCATE PREPARE p42_add_session_auth;

SET @p42_has_record_excluded := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'record'
    AND COLUMN_NAME = 'agent_memory_excluded'
);
SET @p42_add_record_excluded_sql := IF(
  @p42_has_record_excluded = 0,
  'ALTER TABLE `record` ADD COLUMN `agent_memory_excluded` TINYINT(1) NOT NULL DEFAULT 0 AFTER `cover_attachment_id`',
  'SELECT ''record.agent_memory_excluded exists'''
);
PREPARE p42_add_record_excluded FROM @p42_add_record_excluded_sql;
EXECUTE p42_add_record_excluded;
DEALLOCATE PREPARE p42_add_record_excluded;

SET @p42_has_record_note := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'record'
    AND COLUMN_NAME = 'agent_memory_context_note'
);
SET @p42_add_record_note_sql := IF(
  @p42_has_record_note = 0,
  'ALTER TABLE `record` ADD COLUMN `agent_memory_context_note` VARCHAR(255) NULL AFTER `agent_memory_excluded`',
  'SELECT ''record.agent_memory_context_note exists'''
);
PREPARE p42_add_record_note FROM @p42_add_record_note_sql;
EXECUTE p42_add_record_note;
DEALLOCATE PREPARE p42_add_record_note;

CREATE TABLE IF NOT EXISTS `agent_memory_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `session_id` BIGINT NOT NULL,
  `assistant_message_id` BIGINT NOT NULL,
  `source_record_id` BIGINT NULL,
  `source_kind` VARCHAR(24) NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_memory_source_message_record_kind` (`assistant_message_id`, `source_record_id`, `source_kind`),
  KEY `idx_agent_memory_source_session` (`session_id`, `assistant_message_id`),
  KEY `idx_agent_memory_source_user` (`user_id`, `session_id`),
  CONSTRAINT `fk_agent_memory_source_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_memory_source_session_id`
    FOREIGN KEY (`session_id`) REFERENCES `agent_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_memory_source_message_id`
    FOREIGN KEY (`assistant_message_id`) REFERENCES `agent_message` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_memory_source_record_id`
    FOREIGN KEY (`source_record_id`) REFERENCES `record` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT
  SUM(CASE WHEN `cross_record_memory_enabled` = 0 THEN 1 ELSE 0 END) AS session_auth_false_count,
  SUM(CASE WHEN `cross_record_memory_enabled` = 1 THEN 1 ELSE 0 END) AS session_auth_true_count
FROM `agent_session`;

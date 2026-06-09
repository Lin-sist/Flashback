-- M3 local demo schema repair.
-- Non-destructive: adds only missing M3 columns/table required by the current backend.
-- Run against the local flashback database after older demo databases fail on M3 fields.

SET @database_name := DATABASE();

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'openid') = 0,
  'ALTER TABLE `user` ADD COLUMN `openid` VARCHAR(100) DEFAULT NULL AFTER `avatar`',
  'SELECT ''user.openid exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_user_openid') = 0,
  'ALTER TABLE `user` ADD UNIQUE KEY `uk_user_openid` (`openid`)',
  'SELECT ''uk_user_openid exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'record' AND COLUMN_NAME = 'belief_then') = 0,
  'ALTER TABLE `record` ADD COLUMN `belief_then` TEXT DEFAULT NULL AFTER `ai_prompt_result`',
  'SELECT ''record.belief_then exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'record' AND COLUMN_NAME = 'reality_later') = 0,
  'ALTER TABLE `record` ADD COLUMN `reality_later` TEXT DEFAULT NULL AFTER `belief_then`',
  'SELECT ''record.reality_later exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'record' AND COLUMN_NAME = 'reality_later_submit_count') = 0,
  'ALTER TABLE `record` ADD COLUMN `reality_later_submit_count` INT NOT NULL DEFAULT 0 AFTER `reality_later`',
  'SELECT ''record.reality_later_submit_count exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'record' AND COLUMN_NAME = 'life_node_type') = 0,
  'ALTER TABLE `record` ADD COLUMN `life_node_type` VARCHAR(30) DEFAULT NULL AFTER `reality_later_submit_count`',
  'SELECT ''record.life_node_type exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name AND TABLE_NAME = 'record' AND COLUMN_NAME = 'life_node_custom_label') = 0,
  'ALTER TABLE `record` ADD COLUMN `life_node_custom_label` VARCHAR(50) DEFAULT NULL AFTER `life_node_type`',
  'SELECT ''record.life_node_custom_label exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `record_reminder` (
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

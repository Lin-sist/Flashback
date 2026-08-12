-- P3.2 data ownership foundation.
-- 只创建 operation / item 状态锚点；不读取或输出用户原文、位置、对象 key 或 URL。

-- Read-only preflight: only schema metadata is returned.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('data_operation', 'data_operation_record')
ORDER BY table_name;

SELECT table_name, index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('data_operation', 'data_operation_record')
ORDER BY table_name, index_name, seq_in_index;

CREATE TABLE IF NOT EXISTS `data_operation` (
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

CREATE TABLE IF NOT EXISTS `data_operation_record` (
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

SELECT operation_type, status, COUNT(1) AS operation_count
FROM data_operation
GROUP BY operation_type, status
ORDER BY operation_type, status;

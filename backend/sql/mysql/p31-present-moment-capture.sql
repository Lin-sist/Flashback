-- P3.1 present-moment-capture
-- 仅输出聚合计数；禁止输出 user_id、record id、正文、位置或媒体元数据。
-- 在执行状态迁移前，应用版本必须已经能够读取 SAVED / MOMENT / draft_expires_at。

-- Preflight：按状态汇总，eligible_count 只判断是否存在可保存证据。
SELECT
  r.status,
  COUNT(*) AS record_count,
  SUM(
    CASE
      WHEN TRIM(COALESCE(r.content, '')) <> ''
        OR EXISTS (
          SELECT 1
          FROM record_attachment ra
          WHERE ra.record_id = r.id
            AND ra.user_id = r.user_id
            AND ra.status = 'AVAILABLE'
            AND ra.type IN ('IMAGE', 'VOICE')
        )
      THEN 1 ELSE 0
    END
  ) AS eligible_count
FROM `record` r
GROUP BY status
ORDER BY status;

SET @draft_expiry_column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'record'
    AND column_name = 'draft_expires_at'
);

SET @draft_expiry_column_ddl = IF(
  @draft_expiry_column_exists = 0,
  'ALTER TABLE `record` ADD COLUMN `draft_expires_at` DATETIME NULL AFTER `unlocked_at`',
  'SELECT ''draft_expires_at already exists'''
);

PREPARE draft_expiry_column_stmt FROM @draft_expiry_column_ddl;
EXECUTE draft_expiry_column_stmt;
DEALLOCATE PREPARE draft_expiry_column_stmt;

ALTER TABLE `record`
  ALTER COLUMN `record_type` SET DEFAULT 'MOMENT';

SET @draft_expiry_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'record'
    AND index_name = 'idx_record_status_draft_expires'
);

SET @draft_expiry_index_ddl = IF(
  @draft_expiry_index_exists = 0,
  'ALTER TABLE `record` ADD INDEX `idx_record_status_draft_expires` (`status`, `draft_expires_at`)',
  'SELECT ''idx_record_status_draft_expires already exists'''
);

PREPARE draft_expiry_index_stmt FROM @draft_expiry_index_ddl;
EXECUTE draft_expiry_index_stmt;
DEALLOCATE PREPARE draft_expiry_index_stmt;

-- 历史有效 DRAFT 是旧产品中的用户记录；保留原 type 与全部关联上下文，只迁移状态。
UPDATE `record` r
SET status = 'SAVED',
    draft_expires_at = NULL
WHERE r.status = 'DRAFT'
  AND (
    TRIM(COALESCE(r.content, '')) <> ''
    OR EXISTS (
      SELECT 1
      FROM record_attachment ra
      WHERE ra.record_id = r.id
        AND ra.user_id = r.user_id
        AND ra.status = 'AVAILABLE'
        AND ra.type IN ('IMAGE', 'VOICE')
    )
  );

-- 只给无法成为完整记录的技术 DRAFT 新的恢复窗口。
UPDATE `record`
SET draft_expires_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
WHERE status = 'DRAFT';

-- Postflight：仍只输出聚合状态与期限分布。
SELECT
  status,
  COUNT(*) AS record_count,
  SUM(CASE WHEN draft_expires_at IS NULL THEN 1 ELSE 0 END) AS no_expiry_count,
  SUM(CASE WHEN draft_expires_at > CURRENT_TIMESTAMP THEN 1 ELSE 0 END) AS active_expiry_count
FROM `record`
GROUP BY status
ORDER BY status;

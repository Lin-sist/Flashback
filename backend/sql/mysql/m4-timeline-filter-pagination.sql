-- M4 时光轴筛选/分页索引：支持 owner-scoped created_at 范围查询与稳定 id tie-breaker。
-- 可重复执行；仅在索引不存在时添加。

SET @timeline_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'record'
    AND index_name = 'idx_record_user_created_id'
);

SET @timeline_index_ddl = IF(
  @timeline_index_exists = 0,
  'ALTER TABLE `record` ADD INDEX `idx_record_user_created_id` (`user_id`, `created_at`, `id`)',
  'SELECT ''idx_record_user_created_id already exists'''
);

PREPARE timeline_index_stmt FROM @timeline_index_ddl;
EXECUTE timeline_index_stmt;
DEALLOCATE PREPARE timeline_index_stmt;

-- C3 agent-memory-retrieval：会话用途列。
-- 决策依据：openspec/changes/agent-memory-retrieval/design.md 决策 8
--   （复用 agent_session 而非另建表；用途列在本刀一次建好，避免后一刀再做一次 DDL）。
--
-- 本刀只产生 'WRITING_GUIDANCE'。'REVIEW_CHAT' 是**声明而非实现**——
-- 后端在本刀不存在任何依赖该值的行为分支，留给 agent-review-chat。
--
-- 向后兼容：默认值使变更前创建的会话自动被视为写作引导用途，无需数据迁移。
-- 可重复执行：仅在列不存在时新增。
--
-- 本刀**不新增任何索引**，尤其不新增全文索引：
-- 记忆检索复用 record 表既有的 (user_id, status, created_at) 复合索引，
-- 且不匹配 record.content（proposal Q3 定稿、design 决策 5）。

SET @database_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @database_name
     AND TABLE_NAME = 'agent_session'
     AND COLUMN_NAME = 'purpose') = 0,
  'ALTER TABLE `agent_session` ADD COLUMN `purpose` VARCHAR(30) NOT NULL DEFAULT ''WRITING_GUIDANCE'' AFTER `record_id`',
  'SELECT ''agent_session.purpose exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

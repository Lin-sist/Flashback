-- P4.1 witness-agent-alignment
-- 只迁移结构化 session 状态；不读取或输出会话、日记、记忆或模型文本。

-- Read-only preflight: schema + structured aggregate only.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'agent_session'
  AND COLUMN_NAME IN ('purpose', 'conversation_intent', 'stage', 'stage_reask_count')
ORDER BY ORDINAL_POSITION;

SET @p41_has_intent := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agent_session'
    AND COLUMN_NAME = 'conversation_intent'
);
SET @p41_add_intent_sql := IF(
  @p41_has_intent = 0,
  'ALTER TABLE `agent_session` ADD COLUMN `conversation_intent` VARCHAR(24) NULL AFTER `purpose`',
  'SELECT ''agent_session.conversation_intent exists'''
);
PREPARE p41_add_intent FROM @p41_add_intent_sql;
EXECUTE p41_add_intent;
DEALLOCATE PREPARE p41_add_intent;

UPDATE `agent_session`
SET `conversation_intent` = 'LISTEN'
WHERE `purpose` = 'WRITING_GUIDANCE'
  AND `conversation_intent` IS NULL;

UPDATE `agent_session`
SET `stage` = 'WITNESS',
    `stage_reask_count` = 0
WHERE `purpose` = 'WRITING_GUIDANCE'
  AND `status` = 'ACTIVE'
  AND `stage` NOT IN ('WITNESS', 'CLOSING');

UPDATE `agent_session`
SET `conversation_intent` = NULL
WHERE `purpose` = 'REVIEW_CHAT';

SELECT `purpose`, COALESCE(`conversation_intent`, 'NONE') AS intent_state, `stage`, COUNT(1) AS session_count
FROM `agent_session`
GROUP BY `purpose`, COALESCE(`conversation_intent`, 'NONE'), `stage`
ORDER BY `purpose`, intent_state, `stage`;

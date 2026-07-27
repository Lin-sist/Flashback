-- C2 agent-tool-calling：Agent 工具提议与执行的审计 / 幂等持久化。
-- 决策依据：openspec/changes/agent-tool-calling/design.md 决策 4
--   （新建独立表，不复用 agent_message —— 其唯一键 uk_agent_message_session_turn_role
--     限制同 session 同 turn 同 role 只能一条，而一轮内可能先提议、再拒绝、再产生新提议；
--     且该唯一键是 C1 失败重试幂等的实现基石，不得改动）。
-- 可重复执行：仅在表不存在时创建。
--
-- 隐私（design.md 决策 6）：args_digest 只保存参数的**结构化摘要**
--   （工具名、tagIds、text 的字符数与哈希前缀），**禁止**保存用户日记原文或对话原文。
--   本表不是被授权存放日记原文的业务存储（agent_message / record.content 才是）。

CREATE TABLE IF NOT EXISTS `agent_tool_call` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `turn_no` INT NOT NULL,
  `tool_name` VARCHAR(50) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',
  `args_digest` VARCHAR(500) DEFAULT NULL,
  -- pending_args：**瞬态**执行参数缓冲，只在 status='PROPOSED' 期间有值，
  -- 提议一经确认 / 拒绝 / 失败即被置为 NULL（见 AgentToolCallMapper.updateStatusIfProposed）。
  -- 之所以需要它：append_record_content 的 text 是执行所必需的入参，
  -- 而 args_digest 按决策 6 只存不可还原的摘要；参数也不能交由前端在 confirm 时回传
  -- （那等于让客户端绕过白名单与校验）。因此保留一个「用完即清」的缓冲，
  -- 使审计表不留下日记文本的**长期**副本。
  `pending_args` TEXT DEFAULT NULL,
  `ask_text` VARCHAR(255) DEFAULT NULL,
  `failure_type` VARCHAR(50) DEFAULT NULL,
  `result_summary` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_tool_call_session_status` (`session_id`, `status`),
  KEY `idx_agent_tool_call_session_id` (`session_id`, `id`),
  KEY `idx_agent_tool_call_user_id` (`user_id`),
  CONSTRAINT `fk_agent_tool_call_session_id`
    FOREIGN KEY (`session_id`) REFERENCES `agent_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_tool_call_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_tool_call_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

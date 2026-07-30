-- C5 agent-observability：Agent 每轮决策轨迹（thought → action → observation）。
-- 决策依据：openspec/changes/agent-observability/design.md
--   决策 1（选 MySQL 表而非 JSON 日志文件——已批准的对蓝图缓解措施的偏离：
--           本地无日志聚合，且 C6 需要 trace_id / prompt_version / policy_version / model 的字段级关联）
--   决策 2（每轮一条聚合记录，步骤明细放 steps_json 单列；埋点收敛到单一落库出口）
-- 可重复执行：仅在表不存在时创建。
--
-- 隐私（design.md §2.3）：本表**不是**被授权存放日记原文的业务存储
--   （agent_message.content / record.content 才是）。
--   本表所有列只允许承载三类值：结构化枚举短标识、数值指标、长度或不可还原的哈希前缀。
--   **禁止**写入用户日记原文、对话原文、记忆片段内容、护栏候选文本、
--   未覆盖片段内容、提示词全文、provider 响应体原文。

CREATE TABLE IF NOT EXISTS `agent_turn_trace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  -- 每轮一个，供 C6 关联同一轮的多处数据；不含任何用户信息。
  `trace_id` CHAR(32) NOT NULL,
  `session_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `turn_no` INT NOT NULL,
  -- attempt_no：同轮重试的第几次尝试。(session_id, turn_no) 在 provider 失败重试时会重复，
  -- 不能靠它区分「重试」与「新一轮」，故单列一个尝试序号（design §2.2）。
  `attempt_no` INT NOT NULL DEFAULT 1,
  -- 会话用途派生的模式：WRITING_GUIDANCE / REVIEW_CHAT。
  `purpose` VARCHAR(30) NOT NULL,
  `stage` VARCHAR(30) NOT NULL,
  -- 阶段判定结论，复用既有 AgentStageDecision.Reason；回看无阶段机时为 NULL，
  -- 不伪造一个不存在的判定结论（agent-runtime delta「无阶段机模式的轨迹」）。
  `stage_reason` VARCHAR(30) DEFAULT NULL,
  `model` VARCHAR(100) DEFAULT NULL,
  -- 版本锚点由提示词模板与护栏规则文案的内容哈希派生（决策 6），
  -- 改文案即自动变化，不依赖人工 bump。
  `prompt_version` VARCHAR(20) DEFAULT NULL,
  `policy_version` VARCHAR(20) DEFAULT NULL,
  -- 本轮结果：SUCCESS / FAILED / UNAVAILABLE / DOWNGRADED。
  `outcome` VARCHAR(20) NOT NULL,
  -- provider 调用耗时。成功路径同样记录（C5 前只有失败路径算耗时，成功路径被丢弃）。
  `provider_duration_ms` BIGINT DEFAULT NULL,
  -- 失败原因只记异常类名，不记异常消息（消息可能回带请求内容）。
  `cause_type` VARCHAR(100) DEFAULT NULL,
  -- 降级路径标识（reply / reply-attribution / material / tool-proposal / ask-text）。
  `downgrade_path` VARCHAR(50) DEFAULT NULL,
  -- 违规类型，取自 AgentGuardrailViolation 的结构化短标识。
  `violation` VARCHAR(50) DEFAULT NULL,
  -- 步骤明细：只装结构化值（枚举短标识、计数、长度、耗时、哈希前缀）。
  -- 不是自由文本容器——收集器在类型层就不接受任意文本内容（design §2.3）。
  `steps_json` TEXT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_turn_trace_session` (`session_id`, `turn_no`, `attempt_no`),
  KEY `idx_agent_turn_trace_user_id` (`user_id`, `id`),
  KEY `idx_agent_turn_trace_created_at` (`created_at`),
  CONSTRAINT `fk_agent_turn_trace_session_id`
    FOREIGN KEY (`session_id`) REFERENCES `agent_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_turn_trace_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_agent_turn_trace_record_id`
    FOREIGN KEY (`record_id`) REFERENCES `record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 保留期清理（N7 / 决策 11）：**手动执行**，本刀不引入定时任务
--   （自动删数据在无备份策略的本地环境风险不对称，且调度触碰「不改 deployment」边界）。
-- 用法：把 90 替换为 app.agent.observability.retention-days 的实际配置值。
--   DELETE FROM `agent_turn_trace` WHERE `created_at` < DATE_SUB(NOW(), INTERVAL 90 DAY);

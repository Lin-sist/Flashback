-- C5 agent-observability：开发者排查用的只读查询。
--
-- 为什么是脚本而不是 HTTP 端点（design.md 决策 4）：
--   `/admin/**` 的鉴权链路虽然完整可用，但规划期核实到 `AuthRole.ADMIN`
--   **全仓没有任何签发路径**（UserServiceImpl 固定签 USER），因此那里的端点
--   在真实环境不可达。要让它可达就得改认证签发——把认证风险塞进一个可观测 change，
--   性价比极差。产品 API 下的 trace 端点则直接违反 C5 非目标
--   「不面向终端用户展示」。
--   同等能力也由 AgentTurnTraceMapper.selectBySessionId 提供，并有集成测试实证。
--
-- 隐私：本表不含任何日记原文 / 对话原文 / 记忆片段，查询结果可安全阅读与粘贴。

-- ============ 1. 某会话的完整决策链路（最常用）============
-- 用法：把 :sessionId 换成实际会话 id。
SELECT
  turn_no,
  attempt_no,
  purpose,
  stage,
  stage_reason,
  outcome,
  provider_duration_ms,
  cause_type,
  downgrade_path,
  violation,
  model,
  prompt_version,
  policy_version,
  created_at
FROM agent_turn_trace
WHERE session_id = 1 /* :sessionId */
ORDER BY turn_no ASC, attempt_no ASC, id ASC;

-- ============ 2. 某会话某轮的步骤明细 ============
-- steps_json 里是 thought → action → observation 的有序步骤。
SELECT turn_no, attempt_no, outcome, steps_json
FROM agent_turn_trace
WHERE session_id = 1 /* :sessionId */
  AND turn_no = 1 /* :turnNo */
ORDER BY attempt_no ASC;

-- ============ 3. 最近发生降级的轮次 ============
-- 排查「Agent 突然说了句无关的话」时先看这里：
-- downgrade_path 说明是哪道闸，violation 说明为什么。
SELECT session_id, turn_no, attempt_no, downgrade_path, violation, created_at
FROM agent_turn_trace
WHERE outcome = 'DOWNGRADED'
ORDER BY id DESC
LIMIT 50;

-- ============ 4. 最近失败与不可用的轮次 ============
SELECT session_id, turn_no, attempt_no, outcome, cause_type, provider_duration_ms, created_at
FROM agent_turn_trace
WHERE outcome IN ('FAILED', 'UNAVAILABLE')
ORDER BY id DESC
LIMIT 50;

-- ============ 5. 同轮重试（attempt_no > 1）============
-- 一轮被重试多次通常意味着 provider 不稳定。
SELECT session_id, turn_no, MAX(attempt_no) AS attempts
FROM agent_turn_trace
GROUP BY session_id, turn_no
HAVING MAX(attempt_no) > 1
ORDER BY attempts DESC
LIMIT 50;

-- ============ 6. provider 耗时分布 ============
-- C5 之前成功路径的耗时被直接丢弃，这条查询在 C5 之后才有数据。
SELECT
  model,
  COUNT(1) AS turns,
  MIN(provider_duration_ms) AS min_ms,
  ROUND(AVG(provider_duration_ms)) AS avg_ms,
  MAX(provider_duration_ms) AS max_ms
FROM agent_turn_trace
WHERE provider_duration_ms IS NOT NULL
GROUP BY model;

-- ============ 7. 按版本锚点分组（C6 回归比对的入口）============
-- 改了 prompt 或护栏文案后，版本值会自动变化（内容哈希派生）。
-- 这条查询就是「同一版本下行为是否一致」的起点。
SELECT prompt_version, policy_version, outcome, COUNT(1) AS turns
FROM agent_turn_trace
GROUP BY prompt_version, policy_version, outcome
ORDER BY prompt_version, policy_version;

-- ============ 8. 回看模式中被 fail-closed 丢弃的工具提议 ============
-- C3b 归档时这条分支未活体触发；若它真发生过，这里能查到。
SELECT session_id, turn_no, attempt_no, created_at, steps_json
FROM agent_turn_trace
WHERE purpose = 'REVIEW_CHAT'
  AND steps_json LIKE '%tools-fail-closed%'
ORDER BY id DESC
LIMIT 20;

-- ============ 9. 保留期清理（手动，N7 / 决策 11）============
-- 不引入定时任务：自动删数据在无备份策略的环境里风险不对称。
-- 把 90 替换为 app.agent.observability.retention-days 的实际值。
-- 先看会删多少：
--   SELECT COUNT(1) FROM agent_turn_trace WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
-- 确认后再执行：
--   DELETE FROM agent_turn_trace WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);

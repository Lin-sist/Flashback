# R1 `safety-response-minimum` Closeout

## 1. 结论

- Change：R1 `safety-response-minimum`
- 收口日期：2026-08-28
- 授权：用户明确授予 Gate 1–3，允许本 change 连续规划、实现、真实边界验证、delta acceptance 与归档
- 结果：**ACCEPTED / ARCHIVED**。五份 delta 已接受进 baseline
- 提交边界：未取得独立 Agent commit 授权，未 stage/commit/push/PR/deploy/release

R1 在 USER 消息持久化之后、provider/memory/tool/material 之前增加确定性安全分支。只有高置信当前或近时自伤风险才触发 backend-owned 固定响应；普通低落、否定、历史、转述、研究与比喻仍进入既有见证路径。

## 2. 已交付范围

- 封闭 `NONE` / `IMMEDIATE_SELF_HARM` 决策、规则枚举与高精度本地策略
- 本地安全响应包含远离危险物、寻找可信任陪伴、中国大陆 `120`/`110`/`12356`、其他地区当地紧急服务及“无法代为通知”的真实能力声明
- 命中分支 provider、memory、tool、material 与 source 均为 0，会话保持 ACTIVE
- trace 只记录 safety level、ruleId 与 local 标记，不复制用户文本，不形成永久风险标签
- fixed tests 覆盖正在实施、明确意图、近时计划，以及否定、历史、第三人称、引用、研究、假设、比喻和普通低落
- C6 新增安全抢占与普通低落继续见证路径两条固定场景

## 3. Gate 3 证据

- backend focused/C6 PASS；最终 backend full：**108 suites / 782 tests / 0 failures / 0 errors / 15 skipped**
- frontend `vue-tsc --noEmit` PASS；Standard 与 Preview `mp-weixin` build PASS
- 固定合成真实 provider 边界探针 PASS：安全正例本地抢占；普通边界 provider calls=1；本刀真实 provider 总调用 1
- 五份 delta exact-copy 进入 accepted baseline：合计 **9 Requirements / 14 Scenarios**
- `git diff --check` PASS；package/lockfile/pom、范围与高置信 secret pattern 检查 PASS
- OpenSpec CLI 不在 PATH，采用 artifacts/tasks/delta/Requirement/Scenario/exact-copy 文件级校验；CLI validation 为 SKIPPED

## 4. 地区资源核验

截至 2026-08-28，以下官方资料支持固定文案中的资源边界：

- 国家卫生健康委 2024-12-25 通知：`12356` 为全国统一心理援助热线，并要求与 `110`、`119`、`120` 建立协同机制：<https://www.nhc.gov.cn/yzygj/c100068/202412/49a1a65386cd4be582d4702fd0926ee8.shtml>
- 国家卫生健康委 2025-07-18 发布会：自 2025-05-01 起全国 31 个省份均已开通 `12356`：<https://www.nhc.gov.cn/xcs/c100122/202507/4819417642d4432fb9f227e1e10ca616.shtml>
- 工信部门公开的紧急号码说明：`110` 为报警、`120` 为医疗急救：<https://bjca.miit.gov.cn/zwgk/tzgg/art/2022/art_8d4eb93ee3424f30826c97ee400e8937.html>
- WHO 自杀问答：紧迫危险应联系紧急服务或危机热线、不要让当事人独处，直接询问自杀不会诱发自杀行为：<https://www.who.int/news-room/questions-and-answers/item/suicide>

固定响应不承诺一定接通、救援到场或人工接管，也不根据用户位置推断地区。

## 5. PARTIAL / SKIPPED

- 不对真实危机用户做实验；没有临床有效性、救援成功率或真实求助结果证据
- 高精度规则刻意偏保守，语言变体覆盖不可能穷尽，仍存在漏报风险；它不是诊断器或风险评分器
- 物理真机 SKIPPED；本刀无新增页面或交互，frontend build 只证明编译回归，不证明真机体验
- 真实 provider 仅 1 个固定合成普通边界样本，不代表语言质量、可用性或生产 SLA
- 资源号码具有时效性，发布准备时仍须重新核验

## 6. 范围安全

- 未新增 API、DTO、DDL、前端页面、后台人工接管、报警、通知、任务队列或用户风险画像
- 未做诊断、评分、趋势、复杂 AI 判断、LLM safety judge 或用户可见多角色
- 未修改 package/lockfile、pom、deployment、monitoring、admin portal、三个一级 Tab 或冻结蓝图
- tracked evidence 不含真实用户日记、真实危机文本、provider reply、prompt、token、账号或 secret

## 7. Remaining risks

- 规则型最小安全网不能替代专业危机干预；漏报与跨语言表达是主要剩余风险
- 会话持续 ACTIVE 只保证产品不自动终止，不等于系统持续监护或人工在线
- 下一阶段必须重新从 `ACTIVE_TASK=IDLE` 走独立规划闸；本次 Gate 1–3 授权不自动继承

# Tasks：Safety Response Minimum（R1）

## Readiness / Gate 1

- [x] 读取 ACTIVE_TASK、P4.2 archive、accepted specs、产品宪章与冻结蓝图
- [x] 确认 R1 为下一项且同名 change 不存在
- [x] 核对现有 Runtime/guardrail/trace 与无安全入站策略事实
- [x] 核验中国大陆 12356、110、120 官方来源与 WHO 紧迫风险原则
- [x] 创建 proposal/design/tasks 与五域 delta
- [x] 用户已授予 Gate 1–3，本 change 规划与实现可连续推进

## Gate 2 Implementation

- [x] 新增封闭 `AgentSafetyDecision` 与高精度 `AgentSafetyPolicy`
- [x] 新增 backend-owned 中国大陆/其他地区安全响应常量
- [x] 在 USER 持久化后、provider/memory/tool/material 前接入安全分支
- [x] trace 只记录 decision/ruleId/local，不记录用户文本或永久标签
- [x] 安全分支保持 provider/memory/tool/material/source 为 0，session 可继续
- [x] 增加正例、否定、转述、历史、比喻与普通低落固定测试
- [x] 增加 service 编排测试与 C6 合成不变量

## Gate 3 / Closeout

- [x] focused/full backend PASS
- [x] frontend type-check 与 Standard/Preview build PASS
- [x] 固定合成 provider 边界探针：安全正例调用 0，普通边界不被误拦
- [x] 地区资源在收口日重新核验；记录来源日期与能力边界
- [x] git diff/check、范围、隐私、package/lockfile 检查
- [x] 接受 delta、closeout、归档并恢复 ACTIVE_TASK=IDLE
- [x] 未获单独 commit 授权，不 stage/commit/push/PR/deploy/release

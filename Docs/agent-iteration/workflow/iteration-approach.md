# Flashback 迭代思路说明

> 文档性质：当前项目「应该如何迭代」的思路总览  
> 读者：后续编写迭代蓝图的 Claude / 人类规划者  
> 状态日期：2026-07-26  
> 配套：`vibecoding-playbook.md`、`agent-control-model.md`、`../roadmap/`（往哪走，待写）  
> **落地进度**：强制摘要已注入 `AGENTS.md`；checklist / Current Progress / LOG 模板已补；蓝图仍待写。

---

## 1. 一句话目标

把 Flashback 从「有 OpenSpec 与大里程碑（M1–M4）」推进为：

**Spec 驱动、一次一个可验收切片、决策可答辩、执行可追溯、范围可回滚** 的个人工程迭代方式。

这不是为了流程表演，而是为了：

1. **人始终能控制 Agent**（先审计划，再放行代码）；
2. **人始终能理解 Agent**（为什么这样决策、做了什么、验了什么）；
3. **工程证据可复用**（面试叙事、复盘、跨 Agent 交接）。

---

## 2. 从 RAG 汲取的优点（迁移清单）

以下优点来自 RAG 项目已验证的协作实践，**业务内容不迁移，控制机制迁移**。

| # | RAG 优点 | Flashback 应如何落地 |
|---|---|---|
| 1 | 事实源优先级写死 | 保持并强化：`AGENTS.md` → `ACTIVE_TASK` → active change → baseline specs → 代码 → 旧 Docs |
| 2 | Type A / B / C 分级 | 只读、小修、重大变更分流；避免一切改动都建巨型 change |
| 3 | 事前闸门 | Type C 先 proposal/design/tasks/delta，批准后再实现；规划批准 ≠ 实现授权 |
| 4 | 一次一个 active change | `ACTIVE_TASK` 唯一指针；M 大包收口后转向细切片 |
| 5 | design 决策记录 | 每个真实岔路口写「选择 / 为何 / 放弃代价」；服务理解与控制 |
| 6 | AGENT_LOG 只追加执行证据 | 范围、验证、跳过、风险、commit 状态；不写成长篇设计 |
| 7 | 用户故事大白话 | 「改前会发生什么坏事 → 改后有什么不同」 |
| 8 | 能力五态分类 | `confirmed / partial / planned / out_of_scope / unknown`，禁止把 unknown 当 confirmed |
| 9 | 外部调用闸 | 批量 AI / 存储探测 / 付费调用前披露预算并授权 |
| 10 | 验收诚实语义 | 跳过写 SKIPPED 与原因；禁止 mock 冒充真实成功（与现有 AGENTS 一致） |
| 11 | 提交责任前置 | Agent 提交 vs 用户手动提交；默认用户提交；push 另授权 |
| 12 | 方向蓝图与执行 change 分离 | 蓝图管序列；OpenSpec change 管本轮契约 |

### 明确不照搬

- **不**复制 RAG 的 C1–C16 业务序列（认证/Milvus/rerank 等）。
- **不**默认把每个 UI 微调都做成 Type C。
- **不**把 RAG 的评测脚本体系整包搬入，除非未来独立 change 需要 Agent Evaluation。
- **不**在 M4 未收口时并行开启产品 Agent 大改造。

---

## 3. Flashback 当前工程阶段（写蓝图必须认清）

### 3.1 已有底座

| 层 | 现状 | 含义 |
|---|---|---|
| 产品 | 时光回序 V2.0；三 Tab；写下此刻 → 封存/解锁 → 时间回看 | Agent 化不得破坏气质与主路径 |
| OpenSpec | M1 视觉、M2 backend、M3 demo hardening、**M4 real capability**（active） | 执行以 active change 为准 |
| 技术 | Uniapp + Vue3 + Pinia；Spring Boot + MyBatis + MySQL；AI provider；对象存储 | 中等复杂度，适合增量 Agent 能力 |
| 协作 | `AGENTS.md`、`.ai/ACTIVE_TASK`、`AGENT_LOG`、OpenSpec | 骨架已在，需把 RAG 级纪律写清并习惯化 |
| AI 现状 | 单轮 prompt → response 工具调用 | **≠ 产品 Agent**；见 `项目初始分析.md` |

### 3.2 两阶段叙事（蓝图应采用）

```text
阶段 A · 准生产核心可用（当前 M4）
  → 真实 AI / 位置 / 附件 / 封面 / 时光轴筛选分页 / preview 隔离
  → 收口、验收、归档，ACTIVE_TASK → IDLE（或明确下一切片）

阶段 B · Spec 驱动的能力演进（M4 后）
  → 治理加固（可选小 change）+ 产品 Agent 化主线（细切片 Type C）
  → 每个切片：事前闸门 → 实现 → 证据 → 归档
```

**原则：阶段 A 未按 OpenSpec 收口前，不把阶段 B 的产品 Agent 范围混进 M4。**

---

## 4. 推荐的迭代模型（Flashback 版）

### 4.1 控制环（每轮都一样）

```text
① 方向层：iteration-blueprint（待写）回答「序列与依赖」
② 本轮：ACTIVE_TASK 只指向一个 change 或明确 Type B 任务
③ 计划：proposal / design（含决策记录）/ tasks / spec delta
④ 批准：人审边界、非目标、外调预算、提交责任
⑤ 执行：按 tasks 小切片；边做边 AGENT_LOG
⑥ 验收：验证 + diff；SKIPPED 写明；mock 不冒充真实
⑦ 收口：Type C 合入 baseline / archive / IDLE；commit 与 hash 纪律
```

### 4.2 里程碑（M）与细切片（C）如何并存

Flashback 历史使用 **M1 / M2 / M3 / M4** 大里程碑，适合产品阶段冲刺。  
RAG 后期使用 **细 Type C 序列**，适合质量与可答辩工程。

**推荐策略：**

| 场景 | 用法 |
|---|---|
| 当前 M4 未完成 | 继续以 `m4-real-capability-completion` 为 active 事实源；内部 tasks 已是切片则按 tasks 推进，不另开平行 Type C |
| M4 验收归档后 | **新主线改用细切片 Type C**（或明确的 M5 下挂多个子 change，但 **同时只 ACTIVE 一个**） |
| 产品 Agent 化 | 禁止「一个 M5 包含 Runtime + Memory + Eval + Guardrails 全部」；按依赖拆成可独立验收的 change |

蓝图编写时应用「意图卡片」列出序列，并标注建议 change-id 语义与依赖，而不是只写一个口号式 M5。

### 4.3 任务分级（写入蓝图时沿用）

| 类型 | 何时 | 要不要 OpenSpec change | 证据 |
|---|---|---|---|
| **Type A 只读** | 扫描、解释、对照、规划研讨 | 否；不改 ACTIVE_TASK | 通常不写 log；有长期结论可摘要 |
| **Type B 小修** | 文案、注释、已有契约内 bugfix、低风险配置 | 否 | 必须追加 `AGENT_LOG` |
| **Type C 重大** | API/DTO/持久化/权限/AI 语义/用户可见能力/跨模块 | 是：proposal/design/tasks/delta | ACTIVE_TASK + AGENT_LOG + 验收归档 |

**经验法则：改了 API、权限、持久化、AI 行为语义、用户可见主路径 → 一律 Type C。**

---

## 5. 产品 Agent 化在迭代中的位置（给蓝图的方向锚）

`项目初始分析.md` 已给出产品侧判断，蓝图应吸收但**升级为可执行序列意图**，而不是复制散文：

### 5.1 简历叙事分工（保持）

| 项目 | 证明什么 |
|---|---|
| RAG | AI 知道什么：检索、分块、可信、评测 |
| Flashback | AI 能做什么：在克制产品里做陪伴式 Agent（工具、记忆、多轮、护栏） |

### 5.2 建议的能力依赖（蓝图细化时的默认主干）

以下 **不是** 已批准 OpenSpec，而是写蓝图时的推荐依赖方向：

```text
治理习惯对齐（文档/Type 纪律，可 Type B）
  → M4 收口（当前）
  → Agent 对话状态 / 多轮写作引导（最小 Runtime）
  → Tool Calling（草稿、标签、位置、封存等，边界明确）
  → Memory（历史记录检索，叙事重心是 Agent 而非再造 RAG 平台）
  → Guardrails（不诊断、不篡改用户原文、气质约束）
  → Evaluation / 可观测（决策链路、质量维度；可后置）
```

### 5.3 产品非目标（蓝图必须写进 Non-goals）

- 话痨 chatbot、效率仪表盘、社交动态
- 心理诊断 / 复杂评分 dashboard（与 `AGENTS.md` 一致）
- 为 Agent 做大规模 backend rewrite
- 语音转写、生产通知中心、admin、部署监控（除非独立变更且用户批准）
- 用 preview/mock 路径冒充真实用户成功

---

## 6. 证据与诚实：Flashback 必须继承的语义

从 RAG 学到的「报告诚实」比任何模板更重要：

| 语义 | 含义 |
|---|---|
| **confirmed** | 有代码/配置/测试/手验证据 |
| **partial** | 有一部分实现或证据不全 |
| **planned** | 本 change 计划做 |
| **out_of_scope** | 刻意不做 |
| **unknown** | 未核实；**禁止当已实现写进验收** |
| **验证通过** | 命令或手验真实完成 |
| **SKIPPED** | 环境/授权/设备限制导致未跑；写原因与替代验证 |
| **真实路径** | 登录用户路径不得返回 mock success 冒充成功 |

微信小程序场景额外要求：

- 需要真机/开发者工具的验证，在 AGENT_LOG 写清环境与结果；
- 无法手验时写 SKIPPED，不得声称「小程序已验收通过」。

---

## 7. 文件职责速查（写蓝图时勿写错层）

| 文件 / 目录 | 写什么 | 不写什么 |
|---|---|---|
| `AGENTS.md` | 硬规则、必读、禁止项 | 具体功能序列细节 |
| `openspec/project.md` | 项目身份、版本边界 | 逐步任务 checklist |
| `openspec/changes/<id>/` | 本轮范围、设计、任务、delta | 跨多个主线的远景清单 |
| `openspec/specs/` | 已接受长期契约 | 未验收的设想 |
| `.ai/ACTIVE_TASK.md` | 唯一活动指针 | 长期设计 |
| `.ai/AGENT_LOG.md` | 执行证据 | 决策辩论全文 |
| `design.md` 决策记录 | 取舍理由 | 测试输出粘贴 |
| `Docs/agent-iteration/workflow/**` | 协作方法 | 业务 API 字段拍板 |
| `Docs/agent-iteration/roadmap/**` | 序列与依赖（待写） | 直接改代码的授权 |
| `Docs/agent-iteration/项目初始分析.md` | 产品 Agent 化评估 | 当作已批准 M5 scope |

---

## 8. 扬长避短：Flashback 落地时的风险

| 风险 | 表现 | 缓解 |
|---|---|---|
| 大包 change 失控 | M4/M5 无限膨胀 | 收口后改细切片；蓝图拆意图卡片 |
| 流程过重 | 改一行文案也写四件套 | 严格执行 Type A/B/C |
| 两套 Agent 混淆 | 协作 log 与产品 runtime 混谈 | 命名与文档分区 |
| 重复造 RAG | Flashback Memory 做成第二套企业 RAG | 叙事与 scope 锚定「陪伴式工具调用」 |
| 气质漂移 | Agent 过度主动、诊断化 | Guardrails change + 产品初心条款 |
| 文档双源 | Docs 与 OpenSpec 长期分叉 | 蓝图声明：冲突以 OpenSpec 为准；批准后的契约进 change |

---

## 9. 给 Claude 的「写蓝图」输入摘要

编写 `roadmap/iteration-blueprint.md` 时，请输出类似 RAG 蓝图的结构，但内容必须是 Flashback：

1. **阅读与执行约定**（蓝图 vs AGENTS vs OpenSpec；Type B/C 适用范围）
2. **迭代总方向**（准生产 → 可信陪伴式 Agent 能力，而不是通用 Agent 平台）
3. **已确认决策**（从 M4 用户决策、AGENTS 禁止项、初始分析中可升级为「作者已确认」的条目；**不确定的标为待确认**）
4. **change 序列总览表**（含依赖；标注 Type B/C；旁支与主线分离）
5. **每个 change 的意图卡片**（现状 / 目标 / 用户故事 / 非目标 / 验收证据类型）
6. **spec delta 建议落点**（backend-core / miniapp-core / v2-product-scope / agent-collaboration / 未来 agent-runtime 等）
7. **外调与隐私闸**（AI provider、对象存储、用户日记内容不得进普通日志/tracked secrets）

完整编写清单见 `../roadmap/README.md`。

---

## 10. 本阶段文档工作的完成定义

- [x] 说清 Flashback **如何** Spec 驱动迭代
- [x] 说清从 RAG **学什么、不学什么**
- [x] 蓝图编写规格（`../roadmap/README.md`）
- [x] **执行层注入**：`AGENTS.md` Type/Gates/Handoff/LOG；Type B/C checklist；Current Progress；skills 引用
- [x] 产出 `iteration-blueprint.md` v1 草案（2026-07-27；待用户审阅后冻结）
- [ ] **尚未** 用本工作流完整跑通一个 post-M4 Type C（落地检验）

**下一步（人类触发）**：

1. 审阅并冻结 `roadmap/iteration-blueprint.md` v1；  
2. 开启第一个 post-M4 Type C：`agent-runtime-mvp`（用 `type-c-checklist.md`）。

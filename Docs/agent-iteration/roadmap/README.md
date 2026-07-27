# Roadmap 目录说明（待 Claude 编写蓝图）

> 状态：`iteration-blueprint.md` **v1.1 已冻结**；**M4 已归档**；`ACTIVE_TASK=IDLE`  
> 状态日期：2026-07-27  
> 工作流见 `../workflow/`；执行硬规则见仓库根 `AGENTS.md`。  
> **下一步**：启动 C1 `agent-runtime-mvp` 的 **OpenSpec 规划闸**（proposal/design/tasks/delta）；规划批准前禁止业务代码。

---

## 1. 你要产出什么

请在本目录创建：

```text
Docs/agent-iteration/roadmap/iteration-blueprint.md
```

可选后续（非第一版必须）：

```text
Docs/agent-iteration/roadmap/technical-debt.md   # 从代码与 M4 收口中提炼的债务库存
```

**蓝图是方向母文档 / 宪章，不是可执行 OpenSpec change。**  
正式开工某一项时，仍须：

1. 在 `openspec/changes/<change-id>/` 建 proposal/design/tasks/delta；  
2. 将 `.ai/ACTIVE_TASK.md` 指向该 change；  
3. 通过事前闸门后再写业务代码。

---

## 2. 写蓝图前必读

按顺序：

1. 仓库根 `AGENTS.md`
2. `.ai/ACTIVE_TASK.md`（确认 M4 是否仍为 active）
3. `openspec/project.md`
4. `openspec/changes/m4-real-capability-completion/`（若仍在进行，蓝图不得改写其 scope，只能「其后接棒」）
5. `Docs/agent-iteration/workflow/iteration-approach.md`
6. `Docs/agent-iteration/workflow/vibecoding-playbook.md`
7. `Docs/agent-iteration/workflow/agent-control-model.md`
8. `Docs/agent-iteration/项目初始分析.md`

参考结构（**不要复制业务项**）：姊妹项目 RAG 的  
`C:\_01_Code\RAG\docs\roadmap\iteration-blueprint.md`。

---

## 3. 建议文档结构（对齐 RAG 蓝图骨架）

```markdown
# 《时光回序》Flashback｜迭代蓝图（Iteration Blueprint）· vN

> 文档性质：长期迭代的母文档 / 宪章，不是可执行 OpenSpec change
> 状态日期：YYYY-MM-DD
> 状态：草案 / 已冻结

## 0. 给 Agent 的阅读与执行约定
- 与 AGENTS.md / OpenSpec 的优先级
- Type A/B/C 哪些要建 change
- 一次一个 active change
- 用户故事大白话要求
- 外调披露与授权

## 1. 迭代总方向
- 一句话总目标
- 主干依赖链（严格串行的主线）

## 2. 作者已确认的决策
- 仅写已在 M4/用户对话/AGENTS 中确认的；
- 未确认的单独列为「待确认」，禁止伪装成已确认。

## 3. change 序列总览
- 表：顺序 | 建议 change-id 语义 | 一句话目标 | Type B/C | 依赖
- 旁支与主线分离
- M4 收口与 post-M4 的分界写清楚

## 4. 每个 change 的意图卡片
对每一项写：
- 现状事实（需可核对，避免空话）
- 目标
- 用户故事（改前坏事 → 改后不同）
- 非目标 / out_of_scope
- 验收证据类型（测试 / 微信手验 / 契约审查）
- 关键风险

## 5. spec delta 建议落点
- 表：Change → backend-core / miniapp-core / v2-product-scope / agent-collaboration / 新建 capability

## 6. 产品初心与 Agent 气质约束
- 安静、私密、克制、温柔
- 禁止诊断化、效率仪表盘化、话痨化

## 7. 修订记录
- 版本与冻结说明
```

---

## 4. 内容硬约束（Flashback 专用）

### 4.1 必须遵守的产品 / 工程边界

- 三 Tab：首页、时光轴、个人中心  
- 用户可见命名：我的记录、时光轴、时间回看  
- 不做 speech-to-text、复杂 AI 诊断 dashboard、admin、生产部署监控（除非独立变更且用户批准）  
- secret 仅 backend；封存后 location/attachments/cover 不可变  
- 真实路径不得 mock success  
- 不做大规模 backend rewrite / major 视觉重建（除非独立批准）

### 4.2 序列纪律建议

1. **先** 完成或明确收口 M4（准生产核心可用）；  
2. **再** 治理习惯对齐（若需要，多为 Type B 文档/log 纪律）；  
3. **再** 产品 Agent 细切片（Runtime → Tools → Memory → Guardrails → Eval）；  
4. Agent 主线 **不要** 与「无关 UI 翻新」「通知中心」「设置页大改」捆绑。

### 4.3 两套 Agent 分轨

| 轨道 | 蓝图中的写法 |
|---|---|
| 协作 / vibecoding | 可引用 `../workflow/**`，不要把 playbook 全文粘进蓝图 |
| 产品 Agent runtime | 用意图卡片拆分；每张卡片可映射未来一个 Type C |

### 4.4 诚实性

- 不得把 `项目初始分析.md` 的设想写成「已实现」。  
- 指标类能力（若有）验收话术用「报告结果」，禁止写「必须提升」。  
- 凡依赖真机微信、真实 DeepSeek、真实对象存储的项，标注外调/手验闸。

---

## 5. 第一版蓝图的建议范围（可调整，须在文中声明）

**建议纳入主线讨论的主题（示例，非最终）：**

- M4 closeout 条件与归档定义  
- post-M4：Agent 多轮写作引导（最小状态机）  
- Tool calling 白名单与禁止工具  
- 基于历史记录的 memory 检索（克制版）  
- Guardrails（不诊断、不覆写用户原文）  
- Agent 决策链路可观测（工程向）  
- Evaluation 最小集（后置可接受）

**建议明确旁支 / 后置：**

- 设置页、运营活动、生产通知中心  
- 通用多租户 Agent 平台  
- 完整 RAG 中台化 Memory  

---

## 6. 完成定义（蓝图 v1）

当 `iteration-blueprint.md` 满足以下条件时，可请用户「冻结」：

- [ ] 含 §0 执行约定与 Type 分级适用范围  
- [ ] 主线依赖链无矛盾（不要求两个 Type C 并行）  
- [ ] M4 与 post-M4 分界清楚  
- [ ] 每个主线 change 有意图卡片与非目标  
- [ ] 产品初心 / Agent 气质约束独立成节  
- [ ] 未确认决策不与已确认决策混表  
- [ ] 声明：本文件不授权直接改代码  

冻结后：更新本 README 状态行，并在 `../README.md` 阅读顺序中标注「蓝图已冻结」。

---

## 7. 明确不要做的事

- 不要在写蓝图时修改 M4 的 acceptance 来「塞进」Agent 范围。  
- 不要创建多个 active OpenSpec change。  
- 不要把 RAG 的 rerank/Milvus/C12 序列改名后当作 Flashback 蓝图。  
- 不要删除或改写 `../workflow/**` 来迁就未批准的产品幻想。  
- 不要在蓝图中写入真实 API key、用户数据或本机绝对秘密路径。

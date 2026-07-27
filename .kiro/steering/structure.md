# 项目结构（Kiro Steering）

## 仓库根目录

```
Flashback/
├── AGENTS.md                    # 协作硬规则（最高权威）
├── .ai/
│   ├── ACTIVE_TASK.md           # 当前活动任务状态（每次会话必读）
│   └── AGENT_LOG.md             # 执行证据日志（只追加）
├── .kiro/steering/              # Kiro 桥接层（本目录；指向 AGENTS.md）
├── .agent/skills/               # Antigravity OpenSpec skills
├── .claude/skills/              # Claude Code OpenSpec skills
├── openspec/
│   ├── project.md               # 产品身份
│   ├── specs/                   # Baseline specs（已接受的契约）
│   │   ├── backend-core/
│   │   ├── miniapp-core/
│   │   ├── v2-product-scope/
│   │   └── agent-collaboration/
│   └── changes/                 # OpenSpec 变更管理
│       ├── <active-change>/     # 当前实施中的 change（若有）
│       └── archive/             # 已归档的 change（含 M4）
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/flashback/
├── frontend/                    # Uniapp + Vue 3 前端
│   └── src/
│       ├── pages/               # 页面组件
│       ├── components/          # 共享组件
│       ├── stores/              # Pinia stores
│       ├── services/            # API 调用层
│       └── utils/               # 工具函数
└── Docs/
    └── agent-iteration/
        ├── roadmap/
        │   └── iteration-blueprint.md  # 已冻结蓝图 v1.1
        └── workflow/                   # 协作方法论
            ├── vibecoding-playbook.md
            ├── agent-control-model.md
            └── prompt-snippets/        # Type B/C checklist、决策记录模板
```

## 关键路径速查

| 需求 | 去哪里 |
|---|---|
| 了解当前能做什么 | `.ai/ACTIVE_TASK.md` |
| 看 Non-Negotiable 规则 | `AGENTS.md` |
| 看已有 API 契约 | `openspec/specs/backend-core/spec.md` |
| 看前端页面契约 | `openspec/specs/miniapp-core/spec.md` |
| 看产品范围 | `openspec/specs/v2-product-scope/spec.md` |
| 看 Agent 方向蓝图 | `Docs/agent-iteration/roadmap/iteration-blueprint.md` |
| 开 Type C change | `openspec/changes/<change-id>/` 新建 |
| 写执行证据 | `.ai/AGENT_LOG.md` |

## 命名约定

- OpenSpec change-id：kebab-case（如 `agent-runtime-mvp`）
- 后端包路径：`com.flashback.*`
- 前端页面路径：`pages/<feature>/<feature>.vue`
- 前端 store：`stores/<name>.ts`

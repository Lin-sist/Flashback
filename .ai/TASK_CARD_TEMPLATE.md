# Task Card Template

## Task

一句话说明本轮任务。

## Type

A（只读）/ B（小修）/ C（重大 · 须 OpenSpec change）

## Agent

建议执行工具 / 模型。

## Allowed Files

列出白名单文件。

## Forbidden Files

列出禁止文件。

## Must Read

列出本轮必须读取文件，不要让 Agent 自由全仓库搜索。  
Type C 默认至少：`AGENTS.md`、`.ai/ACTIVE_TASK.md`（含 Current Progress）、active change artifacts。

## Requirements

逐条列出验收点。

## Stop Conditions

列出遇到哪些情况必须停止并报告。

## Verification

列出可行验证命令。

## Current Progress（会话结束时回写 ACTIVE_TASK）

- Last session:
- Completed this session:
- Blocked on:
- Next step:
- SKIPPED to revisit:

## Required Output

见 `AGENTS.md` Required Output。

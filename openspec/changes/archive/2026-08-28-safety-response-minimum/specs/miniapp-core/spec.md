# Delta Spec：miniapp-core（R1 Safety Response Minimum）

## ADDED Requirements

### Requirement: Mini Program Must Present Safety Response Without Fake Capabilities

小程序 SHALL 在既有 assistant message 表面展示 backend 安全响应，不新增虚假救援状态。

#### Scenario: Safety response visible

- GIVEN backend 返回本地安全响应
- WHEN Agent sheet 渲染
- THEN SHALL 按普通 assistant message 清晰显示
- AND SHALL NOT 显示“已通知”“人工接入”或自动拨号成功状态

#### Scenario: Existing surface preserved

- GIVEN R1 完成
- WHEN 审查小程序
- THEN 三个一级 Tab、canonical naming、witness 入口与 Preview fail-closed SHALL 保持
- AND SHALL NOT 新增诊断、评分、风险档案或后台通知页面

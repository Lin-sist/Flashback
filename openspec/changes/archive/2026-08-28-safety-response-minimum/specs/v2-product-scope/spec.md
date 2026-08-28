# Delta Spec：v2-product-scope（R1 Safety Response Minimum）

## ADDED Requirements

### Requirement: Safety Interruption Must Remain A Narrow Exception

产品 SHALL 只在证据充分、紧迫的人身安全风险下打断普通记录体验。

#### Scenario: Immediate danger

- GIVEN 用户明确表达当前或近时自伤风险
- WHEN Agent 回应
- THEN MAY 中断普通记录并优先现实安全行动
- AND SHALL 不继续浪漫化、抽象哲学讨论或素材整理

#### Scenario: Non-immediate distress

- GIVEN 用户表达低落、失败、迷茫或一般痛苦但无明确紧迫风险
- WHEN Agent 回应
- THEN SHALL 保持 witness role 且不病理化
- AND SHALL NOT 自动展示危机资源清单

### Requirement: Regional Resources Must Be Honest And Scoped

援助信息 SHALL 标注适用地区并在实施/发布前核验。

#### Scenario: Mainland China resources

- GIVEN 文案列出 120、110 与 12356
- WHEN 返回给用户
- THEN SHALL 明确其为中国大陆资源
- AND SHALL 不推断用户位置或承诺热线一定接通

# Delta Spec：agent-collaboration（R1 Safety Response Minimum）

## ADDED Requirements

### Requirement: Safety Evidence Must Use Synthetic Inputs And Explicit Boundaries

R1 验收 SHALL 使用固定合成输入，并区分分类、编排、provider、地区资源和真实救援效果。

#### Scenario: Synthetic matrix

- GIVEN 正例与普通低落、否定、转述、历史、比喻负例
- WHEN 执行离线与授权的小样本验证
- THEN SHALL 记录结构化 decision/count/status
- AND SHALL NOT 保存真实用户危机内容、provider 文本、prompt、token 或 secret

#### Scenario: Provider boundary

- GIVEN 本地安全分支 PASS
- WHEN closeout 描述结果
- THEN SHALL 只声称高置信输入不调用 provider 且返回固定响应
- AND SHALL NOT 声称临床有效、所有表达可识别或生产救援可用

### Requirement: Regional Resource Claims Must Be Reverified

地区资源 SHALL 使用官方来源并在 closeout 或发布前重新核验。

#### Scenario: Mainland China verification

- GIVEN R1 文案包含 12356、110、120
- WHEN 验收
- THEN SHALL 记录国家卫健委/政府部门来源与核验日期
- AND 二手文章 SHALL NOT 成为唯一依据

# Mini Program Core Spec Delta：present-moment-capture（P3.1）

> 规划草案。范围：当下保存主路径、SAVED/MOMENT、media-only、恢复草稿、保存后可选封存与 Preview 隔离。

## MODIFIED Requirements

### Requirement: Record States Must Stay Legible / 记录状态 MUST 保持可读

用户侧体验 MUST 区分“上次未完成”的技术恢复草稿、已留下、封存中与已抵达记录。技术 DRAFT MUST NOT 作为普通用户记录状态出现在首页、我的记录或时光轴。

#### Scenario: User browses records / 用户浏览记录

- **WHEN** 页面展示不同状态的完整记录
- **THEN** SAVED 使用“已留下”，SEALED 使用“封存中”，UNLOCKED 使用“已抵达”或既有时间回看语义
- **AND** DRAFT 不进入普通记录列表、计数或时光轴
- **AND** SEALED / UNLOCKED 不能表现得像 SAVED 一样可编辑

#### Scenario: User sees a recovery draft / 用户看到恢复草稿

- **GIVEN** 用户存在未过期的技术 DRAFT
- **WHEN** 用户进入新建编辑器
- **THEN** 小程序 MAY 提示“继续上次未完成的记录 / 放弃”
- **AND** SHALL NOT 把该 DRAFT 称为“已留下”或完整记录

### Requirement: M4 Record Editor Must Support Real Location On Editable Records

The Mini Program SHALL support real location input for active DRAFT and SAVED records. Location SHALL remain optional and SHALL NOT be required before saving a present-moment record.

#### Scenario: User adds current location

- GIVEN the user is editing an active DRAFT or SAVED record
- WHEN the user chooses current location and grants permission
- THEN the Mini Program SHALL save location through the backend-supported real path

#### Scenario: User picks location from map

- GIVEN the user is editing an active DRAFT or SAVED record
- WHEN the user selects a location from the map picker
- THEN the Mini Program SHALL save it through the backend-supported real path

#### Scenario: User enters location manually

- GIVEN the user is editing an active DRAFT or SAVED record
- WHEN the user types a manual location
- THEN the Mini Program SHALL save it through the backend-supported real path
- AND coordinates SHALL NOT be required

#### Scenario: Location is skipped or unavailable

- GIVEN location permission is denied, unavailable, or the user does not want location
- WHEN the user saves a valid text/image/voice record
- THEN location SHALL NOT block save
- AND the editor SHALL remain usable

### Requirement: M4 Record Editor Must Support Real Image Attachments On Editable Records

The Mini Program SHALL support real image attachments for active DRAFT and SAVED records, and an AVAILABLE image SHALL be sufficient evidence for a saved record without text.

#### Scenario: User adds images before writing text

- GIVEN the user starts a new MOMENT with blank text
- WHEN the user selects images within accepted limits
- THEN the Mini Program SHALL create/use a technical DRAFT
- AND compress and upload through the backend authorization
- AND show images as AVAILABLE only after backend verification
- AND SHALL NOT require text before the upload flow

#### Scenario: Image-only record is saved

- GIVEN blank text and at least one AVAILABLE image
- WHEN the user invokes “留下这一刻”
- THEN the Mini Program SHALL call the real save path
- AND SHALL present the record as SAVED if the backend confirms success

#### Scenario: User deletes an editable image

- GIVEN an active DRAFT or SAVED record has an image
- WHEN the user deletes it
- THEN the Mini Program SHALL call the backend-supported path
- AND update UI only after success
- AND SHALL keep the old state with an explicit error if SAVED eligibility would be broken

### Requirement: M4 Record Editor Must Support Real Voice Attachments On Editable Records

The Mini Program SHALL support raw voice attachments for active DRAFT and SAVED records without transcription or voice analysis. An AVAILABLE voice SHALL be sufficient evidence for a saved record without text.

#### Scenario: User records voice before writing text

- GIVEN the user starts a new MOMENT with blank text
- WHEN the user records voice within accepted limits
- THEN the Mini Program SHALL create/use a technical DRAFT
- AND upload the raw voice through the backend authorization
- AND show it as AVAILABLE only after verification
- AND SHALL NOT require text, STT, or Agent analysis

#### Scenario: Voice-only record is saved

- GIVEN blank text and at least one AVAILABLE voice
- WHEN the user invokes “留下这一刻”
- THEN the Mini Program SHALL call the real save path
- AND SHALL present the record as SAVED if the backend confirms success

#### Scenario: User re-records or deletes editable voice

- GIVEN an active DRAFT or SAVED record has voice
- WHEN the user re-records or deletes it
- THEN the Mini Program SHALL use supported backend mutation paths
- AND SHALL preserve SAVED state only when at least one valid text/image/voice remains
- AND SHALL NOT allow mutation after SEALED or UNLOCKED

### Requirement: M4 Cover Must Be Selected From Editable Record Image Attachments

The Mini Program SHALL support cover selection from an active DRAFT or SAVED record's own AVAILABLE images. Cover SHALL remain optional and SHALL NOT make an otherwise empty record eligible for save.

#### Scenario: User selects cover

- GIVEN an active DRAFT or SAVED record has at least one AVAILABLE image
- WHEN the user chooses or changes cover
- THEN the Mini Program SHALL use one of that record's own images
- AND save it through the backend-supported real path

#### Scenario: No image exists

- GIVEN an editable record has no AVAILABLE image
- WHEN the user attempts to add a cover
- THEN the Mini Program SHALL guide the user to add an image first
- AND SHALL NOT upload a standalone cover image

#### Scenario: Record is sealed or unlocked

- GIVEN a record is SEALED or UNLOCKED
- WHEN the record is displayed
- THEN cover SHALL be read-only
- AND mutation controls SHALL NOT be shown as available

### Requirement: Record Editor Must Provide A Passive Agent Conversation Entry

记录编辑页 SHALL 在 active DRAFT 或 SAVED 上提供用户主动触发的写作引导入口，且不改变“留下此刻 → 保存 → 结束”的主路径。

#### Scenario: 用户打开写作引导

- GIVEN 一个已登录用户在编辑 active DRAFT 或 SAVED 记录
- WHEN 用户主动点击对话入口
- THEN 小程序 SHALL 以既有半屏浮层展示写作引导
- AND 用户 SHALL 能随时关闭

#### Scenario: 用户未触发对话

- GIVEN 用户进入记录编辑页
- WHEN 页面完成加载或记录保存成功
- THEN 小程序 SHALL NOT 自动展开对话
- AND SHALL NOT 把 Agent 表现为完成记录的必经步骤

#### Scenario: 不可编辑状态

- GIVEN 记录为 SEALED 或 UNLOCKED
- WHEN 页面决定 Agent 入口
- THEN SHALL NOT 提供写作引导入口
- AND UNLOCKED MAY 继续使用既有独立 REVIEW_CHAT 入口

## ADDED Requirements

### Requirement: Present Moment Capture Must Be Complete With Text Image Or Voice

#### Scenario: Text-only moment is saved

- GIVEN 用户输入至少一句非空文字
- AND 不添加图片或声音
- WHEN 用户点击“留下这一刻”
- THEN 小程序 SHALL 通过 authenticated real path 保存为 MOMENT / SAVED
- AND SHALL NOT 要求标题、分类、人生节点、Agent、地点、标签或未来时间

#### Scenario: Image-only moment is saved

- GIVEN 用户只添加至少一张 backend-confirmed AVAILABLE 图片
- WHEN 用户点击“留下这一刻”
- THEN 小程序 SHALL 保存为 MOMENT / SAVED
- AND SHALL NOT 要求补正文

#### Scenario: Voice-only moment is saved

- GIVEN 用户只添加至少一段 backend-confirmed AVAILABLE 原始声音
- WHEN 用户点击“留下这一刻”
- THEN 小程序 SHALL 保存为 MOMENT / SAVED
- AND SHALL NOT 要求补正文、转写或 AI 分析

#### Scenario: No valid evidence exists

- GIVEN 文字为空且没有 AVAILABLE 图片或声音
- WHEN 用户点击“留下这一刻”
- THEN 小程序 SHALL 保留编辑状态并显示克制的可操作提示
- AND SHALL NOT 用标题、位置、标签、AI 输出或 pending 媒体冒充成功

### Requirement: Save Feedback Must Be Quiet Honest And Non-Coercive

#### Scenario: Save succeeds

- GIVEN backend 已确认记录为 SAVED
- WHEN 小程序呈现成功反馈
- THEN SHALL 在当前页面内显示“这一刻已经留下”或 Gate 1 批准的等义文案
- AND SHALL NOT 默认播放声音或震动
- AND SHALL NOT 自动跳转到封存、分享、Agent 或其他流程
- AND 用户 SHALL 能立即离开

#### Scenario: Save fails

- GIVEN network、validation、media commit 或 backend save 失败
- WHEN 小程序处理结果
- THEN SHALL 保留用户当前输入和可恢复媒体状态
- AND SHALL 提供明确重试或修正提示
- AND SHALL NOT 显示本地 mock success

#### Scenario: E0 evidence boundary is reviewed

- GIVEN E0 没有真实目标用户观察
- WHEN P3.1 的具体动效、折叠层级或保存后交互被审查
- THEN 这些细节 SHALL 被标记为 provisional
- AND SHALL NOT 声称 A、B 或 C 已被用户验证为胜者

### Requirement: Sealing Must Be A Post-Save Optional Path

#### Scenario: User does not seal

- GIVEN 记录已是 SAVED
- WHEN 用户不选择“交给时间”并离开
- THEN 记录 SHALL 继续作为完整可编辑记录存在
- AND 页面 SHALL NOT 暗示还有必做步骤

#### Scenario: User chooses to seal

- GIVEN 记录已是 SAVED
- WHEN 用户主动进入“交给时间”并设置有效未来时间
- THEN 小程序 SHALL 调用既有受支持的 seal path
- AND 成功后 SHALL 将记录呈现为封存中

#### Scenario: Draft tries to seal

- GIVEN 当前记录仍是 DRAFT
- WHEN 用户尚未完成显式保存
- THEN 小程序 SHALL NOT 提供可执行的 seal 主路径
- AND SHALL NOT 将 save 与 seal 静默合并

### Requirement: Recovery Draft Must Not Pretend To Be Saved

#### Scenario: User leaves unfinished editing

- GIVEN 用户有未确认文字、媒体或辅助字段
- WHEN 用户关闭编辑器且恢复持久化成功
- THEN 小程序 MAY 离开并保留技术 DRAFT
- AND SHALL NOT 显示“这一刻已经留下”

#### Scenario: User returns to an unfinished draft

- GIVEN authenticated backend 返回 owner 的未过期 DRAFT
- WHEN 用户再次进入新建编辑器
- THEN 小程序 SHALL 提供继续或放弃选择
- AND DRAFT SHALL NOT 计入我的记录或时光轴

#### Scenario: No meaningful unfinished content exists

- GIVEN 编辑器没有文字、媒体或辅助字段
- WHEN 用户离开
- THEN 小程序 SHALL NOT 创建空技术 DRAFT

### Requirement: Authenticated And Preview Paths Must Remain Isolated

#### Scenario: Authenticated user saves a moment

- GIVEN 用户有真实认证 token 且不在 Preview session
- WHEN 用户创建、更新、上传媒体或保存记录
- THEN 小程序 SHALL 使用 backend-backed real path
- AND SHALL NOT 使用 Preview 数据或本地成功替代

#### Scenario: Preview user attempts mutation

- GIVEN 用户处于显式 Preview 且没有真实 token
- WHEN 用户尝试 create/update/save/seal/media/location/cover/Agent mutation
- THEN 小程序 SHALL fail-closed 并说明 Preview 只读
- AND MAY 展示 MOMENT / SAVED 示例但不得持久化

#### Scenario: Real login replaces Preview

- GIVEN Preview session 存在
- WHEN 真实账号或微信登录成功
- THEN Preview session SHALL 被清除
- AND 后续记录路径 SHALL 只使用真实用户数据

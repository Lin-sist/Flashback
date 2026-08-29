# Mini Program Core Spec Delta：time-chapter-foundation（P5.x）

> 规划草案。范围：“我的记录”内的记录/篇章结构、主动归属、生命周期、删除确认与 Preview 隔离。

## ADDED Requirements

### Requirement: My Records Must Contain Record And Chapter Secondary Views

The Mini Program SHALL keep the existing three primary tabs and provide “记录 / 篇章” as a secondary switch inside “我的记录”.

#### Scenario: User opens My Records

- GIVEN an authenticated user opens 我的记录
- WHEN the page loads
- THEN 记录 SHALL remain the default secondary view
- AND 篇章 SHALL be available without adding a fourth primary tab

#### Scenario: User opens chapter list

- GIVEN the owner has ACTIVE and ENDED chapters
- WHEN the 篇章 view is selected
- THEN the UI SHALL group them as 进行中 and 已结束
- AND each card SHALL show name, status, member count, optional note excerpt, and “片段覆盖时间” when available
- AND SHALL NOT show progress, result, success/failure, AI summary, or automatic cover

### Requirement: Chapter Creation And Membership Must Stay User Initiated

Records SHALL first save independently. The Mini Program SHALL only create or change chapter membership after a separate user action on existing complete records.

#### Scenario: Record is saved

- GIVEN a DRAFT becomes SAVED
- WHEN save succeeds
- THEN the Mini Program SHALL NOT ask for a chapter, recommend one, auto-add it, or open a chapter prompt

#### Scenario: User composes a chapter

- GIVEN the owner selects one or more SAVED, SEALED, or UNLOCKED records in 我的记录
- WHEN the user chooses 组成篇章
- THEN the form SHALL require only a name and MAY accept an optional user note
- AND SHALL show the selected record count before submission

#### Scenario: Record detail manages membership

- GIVEN the owner opens a complete record detail
- WHEN chapter membership actions are shown
- THEN 加入篇章, 移出篇章, or 转移篇章 SHALL be secondary actions
- AND SHALL NOT expose editing of SEALED or UNLOCKED record content or context

#### Scenario: Transfer requires confirmation

- GIVEN a record belongs to chapter A and the owner selects chapter B
- WHEN transfer is prepared
- THEN the UI SHALL explicitly say this is a transfer and show A and B names
- AND only submit the exact source confirmation after the user confirms
- AND cancel SHALL keep the original membership

### Requirement: Chapter Detail Must Support Reversible Lifecycle And Original Record Order

The Mini Program SHALL provide an independent chapter detail page using backend-authoritative state.

#### Scenario: Active chapter is viewed

- GIVEN an owned chapter is 进行中
- WHEN detail loads
- THEN the UI SHALL show name, optional note, member count, “片段覆盖时间”, and member records
- AND SHALL allow ASC or DESC ordering by original record time

#### Scenario: User ends a chapter

- GIVEN the chapter is 进行中
- WHEN the user chooses 结束篇章 and backend confirms
- THEN the UI SHALL show 已结束 and the current end operation time
- AND SHALL NOT call it completed, successful, failed, or archived

#### Scenario: User continues an ended chapter

- GIVEN the chapter is 已结束
- WHEN the user wants to add another record
- THEN the UI SHALL require an explicit 重新打开篇章 action first
- AND reopening SHALL keep the same chapter rather than copy or split it

#### Scenario: User edits container interpretation

- GIVEN a chapter is 进行中 or 已结束
- WHEN the owner edits name or note
- THEN the Mini Program SHALL save only chapter metadata
- AND member record surfaces SHALL remain unchanged

### Requirement: Chapter Deletion Must State That Records Are Preserved

The Mini Program SHALL make chapter deletion scope explicit before submitting the destructive container action.

#### Scenario: User requests chapter deletion

- GIVEN a chapter currently has N members
- WHEN the owner opens delete confirmation
- THEN the UI SHALL display N and the confirmation “只删除篇章，不删除记录”
- AND cancellation SHALL preserve the current chapter and memberships

#### Scenario: Chapter deletion succeeds

- GIVEN backend confirms chapter deletion
- WHEN the user returns to records
- THEN all former member records SHALL remain available according to their original states
- AND SHALL appear as not belonging to any chapter

### Requirement: Preview Chapters Must Be Fixed And Read Only

Preview MAY demonstrate fixed synthetic chapter list and detail, but SHALL NOT persist or call authenticated mutation paths.

#### Scenario: Preview browses a chapter

- GIVEN an explicit Preview session without a real token
- WHEN the user opens chapter list or detail
- THEN the Mini Program MAY show fixed synthetic chapters and records
- AND SHALL visibly identify Preview/example/read-only state

#### Scenario: Preview attempts a mutation

- GIVEN an explicit Preview session without a real token
- WHEN create, edit, add, remove, transfer, end, reopen, or delete is attempted
- THEN the Mini Program SHALL fail closed before any real backend request
- AND SHALL NOT display mock success

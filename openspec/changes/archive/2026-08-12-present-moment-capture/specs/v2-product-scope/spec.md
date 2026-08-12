# V2 Product Scope Delta — Present Moment Capture

## ADDED Requirements

### Requirement: A Present-Moment Record Must Be Complete Without Future Or AI Steps

P3.1 SHALL treat a record as complete when the user explicitly saves non-blank text or at least one owner-scoped AVAILABLE image or raw voice attachment.

#### Scenario: User saves one kind of present-moment evidence

- GIVEN an authenticated user is editing an active DRAFT
- WHEN the DRAFT contains non-blank text, an AVAILABLE image, or an AVAILABLE raw voice attachment
- THEN the user SHALL be able to save it as a complete present-moment record
- AND title, legacy type selection, life node, location, tags, Agent participation, unlock time, and future review SHALL remain optional

#### Scenario: Metadata exists without record evidence

- GIVEN an active DRAFT has only title, location, tags, Agent metadata, pending media, or failed media
- WHEN the user attempts to save it
- THEN the backend SHALL reject the save without claiming that the record is complete

### Requirement: Saved Present-Moment Records Must Have Explicit Product Semantics

P3.1 SHALL add canonical `SAVED` status and `MOMENT` type semantics while keeping technical recovery DRAFTs out of ordinary user-visible collections.

#### Scenario: New record is saved

- GIVEN an authenticated user starts a new ordinary record
- WHEN the record satisfies the save condition and the user explicitly saves it
- THEN it SHALL become `MOMENT / SAVED`
- AND the user-visible state SHALL be expressed as “已留下” rather than technical DRAFT terminology

#### Scenario: Unsaved work is recoverable

- GIVEN the user leaves an active DRAFT before explicit save
- WHEN the user later returns within the bounded recovery period
- THEN the product MAY offer recovery or discard
- AND the DRAFT SHALL NOT appear as a completed record on home, 我的记录, or 时光轴

### Requirement: Save Feedback Must Remain Quiet And Non-Coercive

P3.1 SHALL acknowledge a successful save without turning completion into a streak, score, obligation, or forced next step.

#### Scenario: Save succeeds

- WHEN a record transitions to SAVED
- THEN the page SHALL quietly acknowledge “这一刻已经留下”
- AND it SHALL NOT play default sound, navigate automatically, invoke Agent, seal the record, share it, or pressure the user to continue

#### Scenario: Detailed interaction treatment is evaluated

- GIVEN E0 ended `INCONCLUSIVE / SKIPPED` with zero real participants
- WHEN exact animation, component hierarchy, or persistence duration is chosen
- THEN those details SHALL be treated as provisional and reversible
- AND no A/B/C prototype variant SHALL be described as user-validated

### Requirement: Time Sealing And Agent Help Must Be Optional After Save

Saving a present-moment record SHALL be independently complete; time sealing and Agent assistance SHALL remain user-invoked secondary paths.

#### Scenario: User does not choose a secondary path

- GIVEN a record is SAVED
- WHEN the user leaves without using Agent or “交给时间”
- THEN the record SHALL remain valid, visible, and editable
- AND the product SHALL NOT present an unfinished-step warning

#### Scenario: User chooses to give the record to time

- GIVEN a record is SAVED
- WHEN the user explicitly chooses “交给时间” and supplies a valid future unlock time
- THEN the existing sealing contract MAY transition it to SEALED
- AND the choice SHALL remain separate from the initial save action

### Requirement: P3.1 Must Preserve The Frozen Scope Boundary

P3.1 SHALL implement only present-moment capture and the narrow technical DRAFT lifecycle needed to support it.

#### Scenario: Broader ownership work is proposed

- WHEN work requires arbitrary deletion of SAVED, SEALED, or UNLOCKED records, clear-all, complete export, or a user-facing data ownership center
- THEN it SHALL be deferred to P3.2 unless a separately approved change expands scope

#### Scenario: Broader Agent or audio intelligence work is proposed

- WHEN work requires Agent prompt or provider behavior changes, new memory policy, speech-to-text, voice semantic analysis, AI scoring, diagnosis, or dashboards
- THEN it SHALL remain outside P3.1
- AND existing privacy, Preview isolation, three-tab navigation, and user-visible naming contracts SHALL remain intact

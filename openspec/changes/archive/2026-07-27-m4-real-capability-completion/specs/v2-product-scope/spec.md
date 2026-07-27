# V2 Product Scope Spec Delta

## ADDED Requirements

### Requirement: M4 Must Target Near Production Usability For Core User Functions

M4 SHALL be treated as a real-capability completion milestone for V2.0 Mini Program core functions.

#### Scenario: Agent implements M4 work

- WHEN an Agent starts M4 implementation
- THEN it SHALL prioritize real authenticated user capability over local-only demo behavior
- AND it SHALL keep completion focused on the Mini Program core loop

#### Scenario: Production platform work is proposed

- WHEN work suggests deployment hardening, monitoring, alerting, incident response, admin portal, or settings-page enhancements
- THEN the Agent SHALL defer it outside M4 unless a new OpenSpec change explicitly includes it

### Requirement: M4 May Retain Explicit Preview Mode

M4 MAY retain one-click preview for demonstration, but preview MUST be separated from real user behavior.

#### Scenario: Preview remains available

- WHEN the user enters explicit preview mode
- THEN the Mini Program MAY use preview data to demonstrate the core flow
- AND this SHALL NOT be treated as evidence that the real authenticated path works

#### Scenario: Real mode is used

- WHEN an authenticated user uses M4 core surfaces
- THEN the Mini Program SHALL use real backend-backed data and real integration states
- AND mock success SHALL NOT be accepted as M4 completion

### Requirement: M4 Storage Scope Is Configurable Private Object Storage

M4 SHALL use a provider-neutral backend contract for record media, with Qiniu and S3-compatible object storage as supported implementations.

#### Scenario: Media storage is implemented

- WHEN images, voice files, or covers are implemented
- THEN they SHALL use the configured object-storage provider with private bucket assumptions
- AND backend-controlled upload authorization and private-access-safe media retrieval SHALL be used

#### Scenario: Active storage provider is changed

- WHEN backend configuration selects Qiniu or an S3-compatible provider
- THEN new uploads SHALL switch provider without changing the attachment APIs or Mini Program business flow
- AND provider-specific features unrelated to record attachment storage SHALL remain out of M4 scope

### Requirement: M4 Voice Scope Is Raw Audio Storage Only

M4 SHALL store and play voice files, but SHALL NOT transcribe or semantically analyze voice.

#### Scenario: Voice feature is implemented

- WHEN the user records voice for a record
- THEN the system SHALL save the raw voice file and allow playback
- AND it SHALL NOT require speech-to-text, transcript search, or voice AI analysis for M4 acceptance

### Requirement: M4 Cover Scope Is Attachment-Based

M4 SHALL support record cover selection only from image attachments already associated with the record.

#### Scenario: User wants a cover

- WHEN a cover is added or changed
- THEN the selected cover SHALL come from the record's own image attachments
- AND a standalone cover upload flow SHALL remain outside M4

### Requirement: M4 Must Preserve Core Product Naming And Tone

M4 SHALL keep the V2.0 Mini Program oriented around private writing and time review.

#### Scenario: Agent updates M4 UI or copy

- WHEN visible copy references records, timeline, review, media, location, or AI
- THEN it SHALL remain quiet, private, and user-centered
- AND it SHALL preserve canonical naming such as "我的记录", "时光轴", and "时间回看"
- AND it SHALL NOT turn the product into a dashboard, social feed, content platform, or admin workflow

### Requirement: M4 Timeline Filtering Must Remain Focused

M4 SHALL improve timeline browsing for larger personal record collections through focused filters and incremental loading.

#### Scenario: Timeline filtering scope is implemented

- WHEN timeline filtering is added
- THEN it SHALL support one tag plus created-time year/month/day selection and pagination
- AND it SHALL preserve the timeline's quiet browsing role
- AND multiple-tag boolean search, keyword search, state/type filtering, and persisted filter preferences SHALL remain outside this M4 addition

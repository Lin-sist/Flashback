# Miniapp Core Delta：Data Ownership Foundation

## ADDED Requirements

### Requirement: Mini Program Must Expose A Real Data And Ownership Page

The authenticated Mini Program SHALL provide a real “数据与所有权” page backed by owner-scoped backend operations.

#### Scenario: User opens data ownership

- **GIVEN** an authenticated real-user session
- **WHEN** the user enters “数据与所有权” from 个人中心
- **THEN** the page SHALL load real summary and operation state from backend
- **AND** SHALL NOT display fixed backup dates, counts, iCloud, automatic backup, restore, PDF, or other unimplemented capabilities

#### Scenario: Page fails to load

- **GIVEN** the backend summary is unavailable
- **WHEN** the page opens
- **THEN** the Mini Program SHALL show an explicit retryable failure state
- **AND** SHALL NOT fall back to demo success data on the authenticated path

### Requirement: Mini Program Export Must Preserve Seal Choice And Real File Delivery

The Mini Program SHALL let the user choose the sealed-content policy and SHALL only report export success after backend artifact completion.

#### Scenario: User starts an export

- **GIVEN** no conflicting operation exists
- **WHEN** the user starts export without changing the default
- **THEN** the request SHALL use `RESPECT_SEAL`
- **AND** choosing `FULL_CONTENT` SHALL require an explicit user action with a clear explanation

#### Scenario: Export is still running or failed

- **GIVEN** backend status is pending, running, retry-required, failed, or expired
- **WHEN** the page renders operation state
- **THEN** the Mini Program SHALL display the real state and available action
- **AND** SHALL NOT show a downloadable-success state

#### Scenario: Export succeeds in WeChat

- **GIVEN** backend status is successful and unexpired
- **WHEN** the user saves the export
- **THEN** the Mini Program SHALL deliver the real ZIP through the supported WeChat file path
- **AND** build output or desktop-only download SHALL NOT be treated as WeChat acceptance

### Requirement: Mini Program Must Confirm Single And Clear-All Deletion

The Mini Program SHALL expose deletion for every record state and SHALL make the scope and irreversibility visible before confirmation.

#### Scenario: User deletes one record

- **GIVEN** the user owns a DRAFT, SAVED, SEALED, or UNLOCKED record
- **WHEN** the user chooses delete from a real record surface
- **THEN** the Mini Program SHALL prepare and confirm an owner-scoped deletion intent
- **AND** SHALL show success only after backend operation success

#### Scenario: User clears all records

- **GIVEN** the ownership summary returns the current owned record count
- **WHEN** the user chooses clear-all
- **THEN** the Mini Program SHALL show the real count, recommend export first, explain irreversibility, and require the backend-issued confirmation phrase
- **AND** SHALL not begin deletion from a single accidental tap

#### Scenario: Deletion requires retry

- **GIVEN** the backend reports `RETRY_REQUIRED`
- **WHEN** the page renders the operation
- **THEN** it SHALL state that some data remains
- **AND** SHALL offer the supported retry action instead of claiming deletion completed

### Requirement: Preview Must Remain Read Only For Ownership Actions

Preview MAY demonstrate the page structure but SHALL NOT create, download, confirm, retry, or complete real ownership operations.

#### Scenario: Preview invokes an ownership mutation

- **GIVEN** there is no authenticated token and an explicit Preview session exists
- **WHEN** export, deletion, confirmation, retry, or download is invoked
- **THEN** the Mini Program SHALL fail closed with an explicit read-only notice
- **AND** SHALL make zero real mutation calls and SHALL NOT generate a fake local ZIP


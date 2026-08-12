# Agent Collaboration Delta：Data Ownership Foundation

## ADDED Requirements

### Requirement: Export Acceptance Must Verify Structure Readability And Media Integrity

P3.2 evidence SHALL verify the produced export as a user-owned artifact rather than relying on endpoint or build success alone.

#### Scenario: Offline export evidence is recorded

- **GIVEN** a synthetic export operation succeeds
- **WHEN** acceptance evidence is collected
- **THEN** it SHALL verify exact package structure, offline HTML, readable Markdown, relative links, media byte length, and SHA-256
- **AND** SHALL distinguish H2/synthetic results from real private-object and WeChat delivery evidence

#### Scenario: Sealed and Agent boundaries are reviewed

- **GIVEN** the package contains sealed records or Agent content
- **WHEN** export evidence is inspected
- **THEN** both sealed policies SHALL be tested
- **AND** user original and Agent content SHALL be proven distinguishable

### Requirement: Deletion Acceptance Must Prove Remote Cleanup And Recovery

P3.2 deletion evidence SHALL prove remote-object handling, database association cleanup, and interruption recovery.

#### Scenario: Database cascade passes locally

- **GIVEN** an H2 or MySQL test shows associated rows were cascaded
- **WHEN** deletion is reported
- **THEN** that result SHALL NOT be used as proof that Qiniu or S3-compatible objects were deleted

#### Scenario: Real deletion probe is authorized

- **GIVEN** Gate 3b authorizes synthetic private-object probes
- **WHEN** deletion acceptance runs
- **THEN** it SHALL cover provider success, not-found idempotency, retryable failure, and restart between remote and database deletion
- **AND** synthetic objects and artifacts SHALL be cleaned afterward

#### Scenario: Clear-all is verified

- **GIVEN** a clear-all operation is tested
- **WHEN** evidence is recorded
- **THEN** it SHALL verify fixed owner snapshot, mutation freeze, progress, retry scope, associated-data absence, and no cross-owner impact

### Requirement: Ownership Evidence Must Preserve Privacy And Gate Boundaries

Planning, implementation, and acceptance evidence SHALL minimize sensitive data and keep external side effects separately authorized.

#### Scenario: Planning artifacts are created

- **GIVEN** Gate 1 planning is in progress
- **WHEN** proposal, design, tasks, or deltas are written
- **THEN** real MySQL, object storage, WeChat, provider, push, deployment, and release SHALL remain unauthorized
- **AND** planning SHALL NOT modify business code

#### Scenario: Verification is skipped

- **GIVEN** OpenSpec CLI, real MySQL, private object storage, or WeChat environment is unavailable or unauthorized
- **WHEN** results are reported
- **THEN** the item SHALL be marked `SKIPPED` with a reason
- **AND** file checks, H2, builds, or desktop download SHALL NOT be promoted as equivalent evidence

#### Scenario: Logs and tracked evidence are reviewed

- **GIVEN** export or deletion evidence is added
- **WHEN** privacy scanning runs
- **THEN** diary content, media content, location detail, storage keys, signed URLs, download tokens, credentials, prompts, and provider responses SHALL be absent


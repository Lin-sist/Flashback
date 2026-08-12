# Agent Runtime Delta：Data Ownership Foundation

## ADDED Requirements

### Requirement: Agent Content In Export Must Remain Distinguishable From User Original

User-visible Agent session content MAY be included in a complete ownership export, but SHALL remain physically and semantically separate from user-authored record content.

#### Scenario: Export includes an Agent session

- **GIVEN** an exported record has owner-visible Agent messages
- **WHEN** the package is built
- **THEN** messages SHALL be written under the Agent section or directory with role and time attribution
- **AND** assistant text SHALL NOT be represented as user-authored diary content

#### Scenario: Internal Agent runtime data is considered

- **GIVEN** prompts, provider responses, guardrail internals, transient tool arguments, eval fixtures, or secrets exist
- **WHEN** the user export is built
- **THEN** they SHALL NOT be copied into the package
- **AND** necessary tool/trace coverage MAY be described only through non-content metadata and counts

### Requirement: Record Deletion Must Remove Record-Linked Agent Data

Deleting a record SHALL remove its record-linked Agent sessions and all dependent messages, tool calls, and turn traces.

#### Scenario: Record with Agent history is deleted

- **GIVEN** the owned record has Agent sessions, messages, tool calls, or traces
- **WHEN** its deletion operation succeeds
- **THEN** all record-linked Agent data SHALL be absent
- **AND** the operation SHALL NOT report success if an associated row remains

#### Scenario: Historical derived data cannot be safely attributed

- **GIVEN** a historical Agent row appears related but lacks a trustworthy record association
- **WHEN** deletion consistency is checked
- **THEN** the system SHALL fail closed or record a repair-required failure
- **AND** SHALL NOT infer linkage from diary text, prompt text, or other sensitive content

### Requirement: Ownership Actions Must Stay Outside Agent Tools

Export, record deletion, and clear-all SHALL remain user-confirmed product actions and SHALL NOT be added to the Agent tool registry.

#### Scenario: User requests deletion in chat

- **GIVEN** a user asks Agent to export, delete a record, or clear all records
- **WHEN** Agent responds
- **THEN** it MAY explain how to reach the real “数据与所有权” page
- **AND** SHALL NOT invoke, propose, or claim execution of the operation

#### Scenario: Model proposes an ownership tool

- **GIVEN** a provider output invents an export or destructive tool call
- **WHEN** the runtime validates the proposal
- **THEN** it SHALL reject the proposal through existing fail-closed boundaries
- **AND** record and operation state SHALL remain unchanged


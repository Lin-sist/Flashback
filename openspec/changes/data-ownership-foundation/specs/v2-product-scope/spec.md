# V2 Product Scope Delta：Data Ownership Foundation

## ADDED Requirements

### Requirement: P3.2 Must Make Record Data Ownership Real

P3.2 SHALL establish real record export, arbitrary record deletion, clear-all, associated-data cleanup, and a user-facing ownership center.

#### Scenario: P3.2 is reported complete

- **GIVEN** the change is proposed for acceptance
- **WHEN** scope is reviewed
- **THEN** authenticated users SHALL be able to obtain an offline readable copy and delete DRAFT, SAVED, SEALED, or UNLOCKED records
- **AND** clear-all and associated private-object cleanup SHALL be real rather than visual placeholders

#### Scenario: Existing fake backup surface is reviewed

- **GIVEN** a page or design contains fixed backup counts, iCloud, automatic backup, restore, or PDF claims
- **WHEN** the authenticated P3.2 surface is implemented
- **THEN** those claims SHALL be removed unless backed by a separately accepted capability
- **AND** only real export and ownership operations MAY be presented as available

### Requirement: Data Retrieval Must Not Erase The Meaning Of Sealing

P3.2 SHALL treat product sealing and owner data retrieval as distinct user choices.

#### Scenario: User does not request full sealed content

- **GIVEN** a record is SEALED and not yet unlocked
- **WHEN** an export uses the default policy
- **THEN** the package SHALL respect the seal and explain the omission

#### Scenario: Owner explicitly requests a complete copy

- **GIVEN** the owner knowingly selects complete retrieval
- **WHEN** the export succeeds
- **THEN** the copy MAY contain the owner's sealed data
- **AND** the in-product record SHALL remain SEALED with unchanged unlock semantics

### Requirement: P3.2 Must Preserve The Frozen Scope Boundary

P3.2 SHALL remain a record-data ownership foundation and SHALL NOT become an account platform, cloud backup service, or publishing system.

#### Scenario: Account lifecycle work is proposed

- **WHEN** work requires account deletion, identity unbinding, openid lifecycle, or removal of all user-level non-record data
- **THEN** it SHALL require a separate Type C change
- **AND** SHALL NOT be silently included in record clear-all

#### Scenario: Backup or publishing expansion is proposed

- **WHEN** work requires automatic cloud backup, restore/import, iCloud, cross-device sync, PDF-only publishing, public sharing, or production disaster recovery
- **THEN** it SHALL remain out of P3.2 unless a separately approved change expands scope

#### Scenario: Agent is asked to execute ownership actions

- **WHEN** the user asks Agent to export, delete, or clear all
- **THEN** Agent MAY direct the user to the ownership UI
- **AND** SHALL NOT execute or claim completion of the operation

#### Scenario: Record reminder data is cleaned

- **WHEN** P3.2 deletes a record and its linked reminder state
- **THEN** that cleanup SHALL remain scoped to the deleted record
- **AND** SHALL NOT imply account deletion, WeChat identity unbinding, or a new subscription-message platform

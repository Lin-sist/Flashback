# Backend Core Delta：Data Ownership Foundation

## ADDED Requirements

### Requirement: Data Ownership Operations Must Be Durable And Owner Scoped

The backend SHALL represent export, single-record deletion, and clear-all as durable operations owned by the authenticated user.

#### Scenario: User reads operation status

- **GIVEN** an authenticated user requests a data ownership operation status
- **WHEN** the operation belongs to that user
- **THEN** the backend SHALL return its real type, state, counts, expiry, retry, and download capabilities
- **AND** SHALL NOT expose another user's operation or data

#### Scenario: Client retries a confirmation

- **GIVEN** a valid deletion intent was already confirmed
- **WHEN** the same confirmation is submitted again
- **THEN** the backend SHALL return the same operation or an equivalent idempotent result
- **AND** SHALL NOT create a second destructive operation

#### Scenario: Operation is interrupted

- **GIVEN** an export or deletion operation stops before completion
- **WHEN** processing resumes
- **THEN** it SHALL continue from persisted owner-scoped state
- **AND** SHALL NOT report success while required items remain pending or failed

### Requirement: Export Must Produce An Offline Readable And Verifiable Copy

The backend SHALL build a private offline export package containing user-owned records, explicit fields, necessary metadata, media, and separately identified Agent content.

#### Scenario: Export package is complete

- **GIVEN** an authenticated user requests an export
- **WHEN** the operation succeeds
- **THEN** the ZIP SHALL contain `index.html`, `records/*.md`, `media/`, `agent/`, `manifest.json`, and `README.txt`
- **AND** `index.html` SHALL work without CDN, fetch, external fonts, scripts, or network access

#### Scenario: Media is exported

- **GIVEN** an exported record owns a private image or voice attachment
- **WHEN** the package is built
- **THEN** the original bytes SHALL be written under a stable relative media path
- **AND** the manifest SHALL include byte length and SHA-256 for verification

#### Scenario: Export cannot read required media

- **GIVEN** a required private media object cannot be read
- **WHEN** package construction reaches that object
- **THEN** the operation SHALL enter a retryable or failed state
- **AND** SHALL remove the partial artifact
- **AND** SHALL NOT present an incomplete package as success

### Requirement: Sealed Export Must Respect Explicit User Choice

Export SHALL default to respecting unopened SEALED content while allowing the authenticated owner to explicitly request a complete copy.

#### Scenario: Default export respects seal

- **GIVEN** the user selects `RESPECT_SEAL` or accepts the default
- **AND** a record remains SEALED and not yet unlocked
- **WHEN** the export is built
- **THEN** only status and necessary seal timing metadata SHALL be exported for that record
- **AND** content, location, media, cover, AI fields, and reply SHALL be omitted with an explanatory placeholder

#### Scenario: User explicitly requests full content

- **GIVEN** the authenticated owner explicitly selects `FULL_CONTENT`
- **WHEN** the export is built
- **THEN** the package SHALL include that owner's complete record data
- **AND** the backend SHALL NOT change record status or unlock the in-product record

### Requirement: Any Record State Must Be Deletable Through One Reliable Flow

An authenticated owner SHALL be able to delete a DRAFT, SAVED, SEALED, or UNLOCKED record through the data ownership operation flow.

#### Scenario: Owner confirms single-record deletion

- **GIVEN** a short-lived owner-scoped deletion intent targets one owned record
- **WHEN** the owner confirms it before expiry
- **THEN** the backend SHALL create one durable deletion operation
- **AND** SHALL prevent further mutation of that target while deletion is active

#### Scenario: Legacy direct draft deletion is invoked

- **GIVEN** a persisted DRAFT may own private media or associated data
- **WHEN** a client invokes the former direct database deletion path
- **THEN** the backend SHALL route through or require the reliable ownership operation
- **AND** SHALL NOT delete the database aggregate while remote objects remain unhandled

#### Scenario: Cross-owner deletion is attempted

- **GIVEN** a user supplies another user's record or intent identifier
- **WHEN** deletion is prepared, confirmed, read, or retried
- **THEN** the backend SHALL reject the request without disclosing target existence or counts

### Requirement: Clear All Must Have A Fixed Scope And Mutation Freeze

Clear-all SHALL delete the authenticated user's record set with an explicit confirmation boundary and deterministic scope.

#### Scenario: Clear-all is prepared

- **GIVEN** an authenticated user requests clear-all preparation
- **WHEN** the backend creates an intent
- **THEN** it SHALL return the owned record count and a short-lived confirmation challenge
- **AND** SHALL NOT begin deletion before confirmation

#### Scenario: Clear-all is confirmed

- **GIVEN** a valid clear-all intent and confirmation text
- **WHEN** the backend accepts confirmation
- **THEN** it SHALL snapshot the owned target records
- **AND** SHALL block record and Agent record-mutation paths while the operation is pending, running, or retry-required
- **AND** SHALL only release a failed operation after explicitly preserving and disclosing the remaining-data state

#### Scenario: Retry follows the original authorization

- **GIVEN** a clear-all operation has retryable failures
- **WHEN** the owner retries it
- **THEN** the backend SHALL retry only the original confirmed snapshot
- **AND** SHALL NOT silently expand deletion to later data

### Requirement: Remote Objects Must Be Removed Before Database Aggregates

Record deletion SHALL remove or confirm absence of every associated private object before deleting the database aggregate.

#### Scenario: Remote object deletion succeeds or is already absent

- **GIVEN** a deletion item owns attachment rows
- **WHEN** each provider reports success or not-found
- **THEN** the backend MAY delete the record aggregate
- **AND** database cascade SHALL remove location, attachment, tag relation, reply, reminder, unlock evidence, and record-linked Agent data

#### Scenario: Record reminder data is removed

- **GIVEN** a deleted record has reminder rows or record-level reminder authorization
- **WHEN** the record deletion succeeds
- **THEN** those record-linked reminder artifacts SHALL be absent
- **AND** this SHALL NOT be represented as account deletion, WeChat identity unbinding, or a production notification-center action

#### Scenario: Remote object deletion fails

- **GIVEN** a provider returns a retryable failure
- **WHEN** the deletion worker processes the item
- **THEN** the record and attachment rows SHALL remain as retry anchors
- **AND** the operation SHALL report remaining data rather than success

#### Scenario: Process stops after remote deletion

- **GIVEN** remote objects were deleted but the database transaction did not complete
- **WHEN** the item is retried
- **THEN** provider not-found SHALL be treated as idempotent success
- **AND** the backend SHALL complete the owner-scoped database deletion

### Requirement: Export And Deletion Artifacts Must Minimize Sensitive Exposure

Temporary artifacts, operation state, logs, and evidence SHALL avoid creating unnecessary sensitive copies.

#### Scenario: Export artifact expires

- **GIVEN** a successful export artifact reaches its configured expiry
- **WHEN** cleanup runs
- **THEN** the private temporary artifact SHALL be removed
- **AND** later download SHALL fail closed as expired

#### Scenario: Operation evidence is written

- **GIVEN** export or deletion processing writes logs or test evidence
- **WHEN** the entry is persisted
- **THEN** it MAY include operation type, counts, failure code, attempt, and duration
- **AND** SHALL NOT include diary content, media content, location detail, storage key, signed URL, download token, credential, prompt, or provider response

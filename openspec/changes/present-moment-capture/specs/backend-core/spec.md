# Backend Core Spec Delta：present-moment-capture（P3.1）

> 规划草案。范围：SAVED/MOMENT、显式保存、恢复 DRAFT、迁移、可编辑矩阵与 SAVED -> SEALED。

## MODIFIED Requirements

### Requirement: Record Lifecycle State Machine

The system SHALL support the core record lifecycle using the states DRAFT, SAVED, SEALED, and UNLOCKED. DRAFT SHALL be a hidden technical recovery state; SAVED SHALL be a complete user record that remains editable until the user seals it.

#### Scenario: Technical draft is editable but not a complete record

- GIVEN a record is in DRAFT state
- AND the authenticated user owns the record
- AND its `draftExpiresAt` is later than the current injected Clock time
- WHEN the user updates it through a supported recovery/edit API
- THEN the system SHALL allow the update
- AND SHALL refresh its recovery expiry according to the accepted 7-day policy
- AND SHALL NOT expose it as a complete record in ordinary home, list, or timeline results

#### Scenario: Draft is explicitly saved

- GIVEN an authenticated user owns an active DRAFT
- AND it has non-blank text or at least one AVAILABLE IMAGE or VOICE attachment
- WHEN the user invokes the supported save command
- THEN the system SHALL atomically transition it to SAVED
- AND SHALL clear `draftExpiresAt`
- AND SHALL preserve the user original content and supporting context

#### Scenario: Save command is retried

- GIVEN a record is already SAVED
- AND the authenticated user owns it
- WHEN the same save command is retried
- THEN the backend SHALL return the current SAVED record as an idempotent success
- AND SHALL NOT create a duplicate record or duplicate transition effect

#### Scenario: Saved record remains editable

- GIVEN a record is SAVED
- AND the authenticated user owns it
- WHEN a supported content or context update leaves non-blank text or at least one AVAILABLE IMAGE or VOICE
- THEN the system SHALL allow the update
- AND the record SHALL remain SAVED

#### Scenario: Saved record would become empty

- GIVEN a record is SAVED
- WHEN an update would leave blank text and no AVAILABLE IMAGE or VOICE attachment
- THEN the backend SHALL reject the operation atomically
- AND the existing record, attachments, cover, and SAVED state SHALL remain unchanged

#### Scenario: Saved record is sealed

- GIVEN a record is SAVED
- AND the authenticated user owns it
- AND its unlock time is later than the current time
- WHEN the user explicitly seals the record
- THEN the system SHALL transition it to SEALED
- AND SHALL preserve the user original content, location, attachments, and cover

#### Scenario: Draft cannot bypass explicit save

- GIVEN a record is DRAFT
- WHEN a user attempts to seal it directly
- THEN the backend SHALL reject the operation
- AND SHALL NOT silently combine save and seal into one transition

#### Scenario: Sealed record is immutable

- GIVEN a record is SEALED
- WHEN a normal user update request attempts to modify its content, location, attachments, cover, or tags
- THEN the system SHALL reject the update
- AND the sealed original content and supporting context SHALL remain unchanged

#### Scenario: Sealed record becomes unlocked

- GIVEN a record is SEALED
- AND its unlock time has arrived
- WHEN the unlock process runs
- THEN the system SHALL transition the record to UNLOCKED
- AND the unlocked record SHALL become available through supported unlocked-record or detail APIs for the owner

#### Scenario: Unlock operation is idempotent

- GIVEN a record is already UNLOCKED
- WHEN the unlock process runs again for the same record
- THEN the system SHALL NOT create duplicate unlock effects
- AND the record SHALL remain UNLOCKED
- AND the operation SHALL be safe to repeat

### Requirement: Record Type Support

The system SHALL support MOMENT as the default type for newly created records while preserving FUTURE_LETTER, NODE_RECORD, and EMOTION_NOTE as accepted historical and optional types.

#### Scenario: New record type is omitted

- GIVEN an authenticated user creates a technical DRAFT
- WHEN record type is omitted
- THEN the backend SHALL use MOMENT
- AND SHALL NOT require the user to choose one of the legacy types before adding text, image, or voice

#### Scenario: Supported record type is accepted

- GIVEN an authenticated user creates or updates an editable record
- WHEN the record type is MOMENT, FUTURE_LETTER, NODE_RECORD, or EMOTION_NOTE
- THEN the system SHALL accept the type if all other validation passes

#### Scenario: Historical types are preserved

- GIVEN an existing record has FUTURE_LETTER, NODE_RECORD, or EMOTION_NOTE
- WHEN P3.1 migration runs
- THEN the migration SHALL preserve that record type
- AND SHALL NOT batch rewrite historical meaning to MOMENT

#### Scenario: Unsupported record type is rejected

- GIVEN an authenticated user submits an unsupported record type
- WHEN the backend validates the request
- THEN the system SHALL reject it through the existing safe validation contract

### Requirement: M4 Configurable Object Storage Must Use Private Object Access

The backend SHALL integrate record media through the existing provider-neutral private object-storage contract. Upload and commit SHALL be allowed for an active DRAFT or SAVED record owned by the authenticated user, while SEALED and UNLOCKED remain immutable.

#### Scenario: Upload token is requested for an editable record

- GIVEN an authenticated user owns an active DRAFT or SAVED record
- WHEN the user requests an upload token for an image or voice attachment
- THEN the backend SHALL validate ownership, editable state, expiry where applicable, media type, count limits, size policy, and key policy
- AND SHALL return a short-lived provider-neutral upload authorization without exposing provider secret keys

#### Scenario: Uploaded object is committed

- GIVEN the Mini Program reports an object uploaded for an active DRAFT or SAVED record
- WHEN the backend commits or verifies the attachment
- THEN the backend SHALL verify the object through the persisted provider
- AND SHALL validate object key, size, type, ownership, and editable state before marking it AVAILABLE
- AND a DRAFT attachment activity SHALL refresh its recovery expiry

#### Scenario: Immutable record requests media mutation

- GIVEN a record is SEALED or UNLOCKED
- WHEN an upload authorization or commit is requested
- THEN the backend SHALL reject the operation
- AND SHALL NOT expose an authorization that could mutate the sealed context

#### Scenario: Media is accessed

- GIVEN an authenticated user requests a record image or voice file
- WHEN the user owns the record and attachment
- THEN the backend SHALL provide a private-access-safe short-lived URL
- AND SHALL NOT require a public bucket

#### Scenario: Active provider is switched

- GIVEN Qiniu and/or S3-compatible credentials are configured backend-side
- WHEN `app.storage.provider` is changed
- THEN new upload authorizations SHALL use the selected provider without frontend business-flow changes
- AND existing attachments SHALL continue to route by their persisted provider while it remains configured

### Requirement: M4 Attachments Must Respect Limits And Editable Lifecycle

The backend SHALL support image and raw voice attachments for active DRAFT and SAVED records with existing limits and private lifecycle rules.

#### Scenario: Attachment limits are enforced

- GIVEN a user adds attachments to an active DRAFT or SAVED record
- WHEN the operation would exceed 9 images, 9 voice files, 40 MB per file, or 300 MB total attachments per record
- THEN the backend SHALL reject the operation
- AND the record attachment state SHALL remain valid

#### Scenario: Editable attachment is deleted

- GIVEN an active DRAFT or SAVED record has an attachment
- AND the authenticated user owns the record
- WHEN the user deletes the attachment through a supported API
- THEN the backend SHALL remove or mark the attachment unavailable according to the existing storage policy
- AND if the deleted image is the current cover, the cover SHALL be cleared atomically
- AND a SAVED record SHALL still satisfy the P3.1 save eligibility invariant after deletion

#### Scenario: Last saved evidence cannot be deleted

- GIVEN a SAVED record has blank text and exactly one AVAILABLE IMAGE or VOICE
- WHEN the user attempts to delete that attachment
- THEN the backend SHALL reject the operation atomically
- AND SHALL preserve the attachment, cover if applicable, and SAVED state

#### Scenario: Sealed attachment mutation is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN a user attempts to add, delete, replace, or re-record an attachment
- THEN the backend SHALL reject the operation
- AND existing attachment metadata SHALL remain unchanged

#### Scenario: Cross-user attachment access is rejected

- GIVEN an attachment belongs to another user's record
- WHEN an authenticated user attempts to read or mutate it
- THEN the backend SHALL reject the operation or return a safe not-found response
- AND private media metadata or URLs SHALL NOT be exposed

### Requirement: M4 Cover Must Come From Same Editable Record Image Attachment

The backend SHALL support an optional cover selected from an active DRAFT or SAVED record's own AVAILABLE image attachments.

#### Scenario: Valid cover is selected

- GIVEN an active DRAFT or SAVED record has an AVAILABLE IMAGE attachment owned by the same user and record
- WHEN the user selects that attachment as cover
- THEN the backend SHALL accept the cover selection
- AND list, timeline, home, or detail responses MAY expose private-access-safe cover metadata

#### Scenario: Invalid cover is rejected

- GIVEN the selected attachment is voice, unavailable, belongs to another record/user, or does not exist
- WHEN the user attempts to set it as cover
- THEN the backend SHALL reject the operation
- AND the previous cover SHALL remain unchanged

#### Scenario: Cover mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN the user attempts to change or clear the cover
- THEN the backend SHALL reject the operation
- AND the cover SHALL remain unchanged

### Requirement: M4 Location Must Support Editable Draft And Saved Records

The backend SHALL support current location, map picker, and manual location for active DRAFT and SAVED records while preserving owner isolation and sealed immutability.

#### Scenario: Editable location is saved

- GIVEN an authenticated user owns an active DRAFT or SAVED record
- WHEN the user saves a valid CURRENT_LOCATION, MAP_PICKER, or MANUAL location
- THEN the backend SHALL persist it
- AND manual input MAY omit latitude and longitude

#### Scenario: Editable location is deleted or updated

- GIVEN an authenticated user owns an active DRAFT or SAVED record with location
- WHEN the user updates or deletes location through a supported API
- THEN the backend SHALL allow the mutation
- AND DRAFT activity SHALL refresh its recovery expiry

#### Scenario: Location mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN a user attempts to add, update, or delete location
- THEN the backend SHALL reject the operation
- AND the sealed location state SHALL remain unchanged

#### Scenario: Unlocked detail includes location

- GIVEN an authenticated user opens time review for an UNLOCKED record
- WHEN the record has location
- THEN the backend SHALL return location data needed for the Mini Program to display it

## ADDED Requirements

### Requirement: Record Save Eligibility Must Use Persisted User Evidence

#### Scenario: Text-only record is eligible

- GIVEN an active DRAFT has non-blank text
- WHEN the owner saves it
- THEN the backend SHALL allow DRAFT -> SAVED even when it has no attachment

#### Scenario: Image-only record is eligible

- GIVEN an active DRAFT has blank text and at least one AVAILABLE IMAGE
- WHEN the owner saves it
- THEN the backend SHALL allow DRAFT -> SAVED

#### Scenario: Voice-only record is eligible

- GIVEN an active DRAFT has blank text and at least one AVAILABLE VOICE
- WHEN the owner saves it
- THEN the backend SHALL allow DRAFT -> SAVED

#### Scenario: Non-evidence fields are insufficient

- GIVEN a DRAFT has only title, type, location, tag, life node, unlock time, AI output, or pending/failed media
- WHEN the owner attempts to save it
- THEN the backend SHALL reject the transition
- AND SHALL explain that text, an available image, or an available voice is required

### Requirement: Technical Draft Recovery Must Be Bounded And Private

#### Scenario: Draft receives a recovery expiry

- GIVEN a DRAFT is created or receives a successful draft activity
- WHEN the backend persists that activity
- THEN `draftExpiresAt` SHALL be set from injected Clock time according to the accepted 7-day policy
- AND ordinary user record queries SHALL NOT include the DRAFT

#### Scenario: Recovery query is owner and expiry scoped

- GIVEN an authenticated user explicitly queries recovery DRAFT records
- WHEN the backend returns results
- THEN it SHALL return only that user's unexpired DRAFT records
- AND SHALL NOT expose expired or cross-user drafts

#### Scenario: Expired draft with remote media is cleaned safely

- GIVEN a DRAFT is expired and has private object-storage attachments
- WHEN cleanup runs
- THEN remote objects SHALL be deleted or confirmed absent before the record row is deleted
- AND a remote failure SHALL retain a retry anchor rather than orphaning an untracked object
- AND cleanup evidence SHALL NOT include content, storage keys, signed URLs, or credentials

#### Scenario: Concurrent activity protects the draft

- GIVEN cleanup selected an expired DRAFT
- WHEN another request refreshes or saves the record before deletion
- THEN expected status and expiry conditions SHALL prevent cleanup from deleting the active or SAVED record

### Requirement: Legacy Draft Migration Must Preserve User Meaning

#### Scenario: Valid legacy draft is migrated

- GIVEN an existing DRAFT has non-blank text or at least one AVAILABLE IMAGE/VOICE
- WHEN the P3.1 migration runs
- THEN it SHALL become SAVED
- AND its existing type, original content, context, and ownership SHALL be preserved

#### Scenario: Invalid legacy draft remains recovery-only

- GIVEN an existing DRAFT has blank text and no AVAILABLE IMAGE/VOICE
- WHEN the migration runs
- THEN it SHALL remain DRAFT
- AND SHALL receive the accepted recovery expiry rather than being promoted

#### Scenario: Migration evidence is private

- GIVEN migration preflight or postflight is reported
- WHEN evidence is written
- THEN it SHALL contain aggregate counts only
- AND SHALL NOT contain user ids, record ids, original content, locations, media metadata, or storage keys

# Backend Core Spec Delta

## ADDED Requirements

### Requirement: M4 Real Paths Must Not Depend On Mock Success

Authenticated backend behavior in M4 SHALL use real implementation paths or explicit unavailable/failure states.

#### Scenario: Real user invokes an integration-backed feature

- GIVEN an authenticated non-preview user invokes AI, storage, media, location, timeline, or time-review behavior
- WHEN the required real integration is unavailable
- THEN the backend SHALL return an explicit unavailable or failure state
- AND it SHALL NOT record or return mock success as if the real operation succeeded

#### Scenario: Preview behavior is used

- GIVEN the request is explicitly in preview or development mode
- WHEN mock or preview data is used
- THEN the response SHALL remain isolated from authenticated real user data
- AND the behavior SHALL NOT be treated as production-ready verification

### Requirement: M4 AI Must Use Configured Real Provider In Real Mode

The backend SHALL support real AI provider calls for M4 AI-supported features.

#### Scenario: AI provider is configured

- GIVEN a supported AI provider base URL, model, and API key are configured
- WHEN the user triggers an AI-supported operation
- THEN the backend SHALL call the configured provider
- AND it SHALL return provider-backed output if the call succeeds
- AND it SHALL preserve the user's original record content

#### Scenario: AI provider is missing or fails

- GIVEN AI provider configuration is missing or the provider call fails
- WHEN the user triggers an AI-supported operation
- THEN the backend SHALL return explicit unavailable or failed behavior
- AND it SHALL NOT return mock output as real provider output
- AND it SHALL NOT block record save or seal when AI is not required

#### Scenario: AI secrets are handled

- GIVEN AI provider credentials exist
- WHEN frontend code or tracked files are reviewed
- THEN API keys SHALL NOT appear in Mini Program code or tracked repository files
- AND provider credentials SHALL be read from backend-side configuration or secret management only

### Requirement: M4 Qiniu Storage Must Use Private Object Access

The backend SHALL integrate Qiniu object storage for record media using a private bucket model.

#### Scenario: Upload token is requested

- GIVEN an authenticated user owns a DRAFT record
- WHEN the user requests an upload token for an image or voice attachment
- THEN the backend SHALL validate ownership, record state, media type, count limits, size policy, and key policy
- AND it SHALL return a short-lived upload authorization without exposing Qiniu secret keys

#### Scenario: Uploaded object is committed

- GIVEN the Mini Program reports an uploaded Qiniu object for a record
- WHEN the backend commits or verifies the attachment
- THEN the backend SHALL verify that the object exists in Qiniu
- AND it SHALL validate object key, size, type, record ownership, and record state before marking the attachment available

#### Scenario: Media is accessed

- GIVEN an authenticated user requests a record image or voice file
- WHEN the user owns the record and attachment
- THEN the backend SHALL provide a private-access-safe URL such as a signed short-lived URL
- AND it SHALL NOT require the Qiniu bucket to be public

### Requirement: M4 Attachments Must Respect Limits And Lifecycle

The backend SHALL support record image and voice attachments with explicit limits and lifecycle rules.

#### Scenario: Attachment limits are enforced

- GIVEN a user adds attachments to a DRAFT record
- WHEN the operation would exceed 9 images, 9 voice files, 40 MB per file, or 300 MB total attachments per record
- THEN the backend SHALL reject the operation
- AND the record attachment state SHALL remain valid

#### Scenario: Draft attachment is deleted

- GIVEN a DRAFT record has an attachment
- AND the authenticated user owns the record
- WHEN the user deletes the attachment through a supported API
- THEN the backend SHALL remove or mark the attachment unavailable according to the implementation policy
- AND if the deleted image is the current cover, the cover SHALL be cleared or replaced only through a valid draft cover operation

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

### Requirement: M4 Cover Must Come From Same Record Image Attachment

The backend SHALL support an optional record cover selected from the record's own image attachments.

#### Scenario: Valid cover is selected

- GIVEN a DRAFT record has an IMAGE attachment owned by the same user and record
- WHEN the user selects that attachment as cover
- THEN the backend SHALL accept the cover selection
- AND list, timeline, home, or detail responses MAY expose cover metadata or a private-access-safe cover URL

#### Scenario: Invalid cover is rejected

- GIVEN the selected attachment is voice, belongs to another record, belongs to another user, or does not exist
- WHEN the user attempts to set it as cover
- THEN the backend SHALL reject the operation
- AND the previous cover SHALL remain unchanged

#### Scenario: Cover mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN the user attempts to change or clear the cover
- THEN the backend SHALL reject the operation
- AND the cover SHALL remain unchanged

### Requirement: M4 Location Must Support Three Input Sources

The backend SHALL support record location through current location, map picker, and manual input.

#### Scenario: Draft location is saved

- GIVEN an authenticated user owns a DRAFT record
- WHEN the user saves a location with source CURRENT_LOCATION, MAP_PICKER, or MANUAL
- THEN the backend SHALL persist the location if validation passes
- AND manual input MAY omit latitude and longitude

#### Scenario: Draft location is deleted or updated

- GIVEN an authenticated user owns a DRAFT record with location
- WHEN the user updates or deletes location through a supported API
- THEN the backend SHALL allow the mutation

#### Scenario: Location mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN a user attempts to add, update, or delete location
- THEN the backend SHALL reject the operation
- AND the sealed location state SHALL remain unchanged

#### Scenario: Unlocked detail includes location

- GIVEN an authenticated user opens time review for an UNLOCKED record
- WHEN the record has location
- THEN the backend SHALL return location data needed for the Mini Program to display it
- AND it SHALL remain scoped to the record owner

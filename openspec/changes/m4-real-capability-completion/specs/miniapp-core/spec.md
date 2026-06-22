# Mini Program Core Spec Delta

## ADDED Requirements

### Requirement: M4 Real Mode Must Use Backend-Backed Data

The Mini Program SHALL use backend-backed data in authenticated real mode for M4 core surfaces.

#### Scenario: Authenticated user opens home, timeline, or time review

- GIVEN the user is authenticated and not in explicit preview mode
- WHEN the user opens home cards, timeline, record detail, or time review
- THEN the Mini Program SHALL request real backend data
- AND it SHALL NOT display preview/mock data as the real user state

#### Scenario: Preview mode is used

- GIVEN the user explicitly enters preview mode
- WHEN preview data is displayed
- THEN the Mini Program MAY use curated preview data
- AND preview state SHALL remain distinguishable from authenticated real mode

### Requirement: M4 Record Editor Must Support Real Location

The Mini Program SHALL support real record location input in the record editor.

#### Scenario: User adds current location

- GIVEN the user is editing a DRAFT record
- WHEN the user chooses current location and grants permission
- THEN the Mini Program SHALL save location through the backend-supported real path

#### Scenario: User picks location from map

- GIVEN the user is editing a DRAFT record
- WHEN the user selects a location from the map picker
- THEN the Mini Program SHALL save the selected location through the backend-supported real path

#### Scenario: User enters location manually

- GIVEN the user is editing a DRAFT record
- WHEN the user types a manual location
- THEN the Mini Program SHALL save the manual location through the backend-supported real path
- AND coordinates SHALL NOT be required for manual input

#### Scenario: Location is unavailable or denied

- GIVEN current location permission is denied or unavailable
- WHEN the user is editing a record
- THEN the Mini Program SHALL allow map picker or manual input where possible
- AND it SHALL NOT block writing the record solely because location is unavailable

### Requirement: M4 Record Editor Must Support Real Image Attachments

The Mini Program SHALL support real image attachments for DRAFT records.

#### Scenario: User adds images

- GIVEN the user is editing a DRAFT record
- WHEN the user selects images within accepted limits
- THEN the Mini Program SHALL compress images by default
- AND upload them through the backend-provided object-storage authorization
- AND show them as available only after backend verification succeeds

#### Scenario: User previews images

- GIVEN a record has available image attachments
- WHEN the user taps an image
- THEN the Mini Program SHALL allow image preview if access authorization succeeds

#### Scenario: User deletes draft image

- GIVEN the user is editing a DRAFT record with an image attachment
- WHEN the user deletes the image
- THEN the Mini Program SHALL call the supported backend path
- AND update the local UI only after the mutation succeeds or show a failure state

### Requirement: M4 Record Editor Must Support Real Voice Attachments

The Mini Program SHALL support real voice attachments as raw audio files.

#### Scenario: User records voice

- GIVEN the user is editing a DRAFT record
- WHEN the user records voice within accepted limits
- THEN the Mini Program SHALL upload the raw voice file through the backend-provided object-storage authorization
- AND show it as available only after backend verification succeeds

#### Scenario: User plays voice

- GIVEN a record has an available voice attachment
- WHEN the user taps play
- THEN the Mini Program SHALL play the voice file if access authorization succeeds

#### Scenario: User re-records or deletes draft voice

- GIVEN the user is editing a DRAFT record with a voice attachment
- WHEN the user re-records or deletes it
- THEN the Mini Program SHALL use supported backend mutation paths
- AND SHALL NOT allow the mutation after the record is sealed

### Requirement: M4 Cover Must Be Selected From Image Attachments

The Mini Program SHALL support record cover selection from the current record's image attachments.

#### Scenario: User selects cover

- GIVEN the user is editing a DRAFT record with at least one available image attachment
- WHEN the user chooses "添加封面" or changes cover
- THEN the Mini Program SHALL allow selecting one of that record's image attachments
- AND save the cover through the backend-supported real path

#### Scenario: No image exists

- GIVEN the DRAFT record has no image attachment
- WHEN the user attempts to add a cover
- THEN the Mini Program SHALL guide the user to add an image first
- AND SHALL NOT upload a standalone cover image in M4

#### Scenario: Record is sealed or unlocked

- GIVEN a record is SEALED or UNLOCKED
- WHEN the record is displayed
- THEN cover is read-only
- AND cover mutation controls SHALL NOT be shown as available actions

### Requirement: M4 Timeline And Home Should Show Real Covers

Timeline and home record cards SHALL show the selected cover when available in real mode.

#### Scenario: Record has cover

- GIVEN a real-mode timeline or home card represents a record with cover
- WHEN the card is rendered
- THEN the Mini Program SHALL display the cover through private-access-safe media access

#### Scenario: Record has no cover

- GIVEN a real-mode timeline or home card represents a record without cover
- WHEN the card is rendered
- THEN the Mini Program SHALL show a safe fallback visual
- AND it SHALL NOT substitute unrelated preview media

### Requirement: M4 Timeline Must Provide Calm Filtered Browsing

The Mini Program SHALL support focused timeline filtering without turning the page into a dashboard or general-purpose search surface.

#### Scenario: User applies filters

- GIVEN the user opens the timeline filter sheet
- WHEN the user selects at most one tag and a year, month, or exact day and applies the filter
- THEN the Mini Program SHALL request page 1 using the accepted tag/date query
- AND tag and date selections SHALL combine with AND semantics
- AND the page SHALL show a compact applied-filter summary

#### Scenario: User resets filters

- GIVEN timeline filters are active
- WHEN the user resets them
- THEN the Mini Program SHALL clear tag/date selections
- AND restart unfiltered timeline loading from page 1

#### Scenario: User loads more records

- GIVEN the current timeline page reports `hasMore`
- WHEN the user reaches the load-more boundary
- THEN the Mini Program SHALL request the next page
- AND merge repeated year-month groups without duplicate record ids

#### Scenario: Filtered result is empty or fails

- GIVEN the user applies a valid filter
- WHEN no records match or the request fails
- THEN the Mini Program SHALL distinguish a filtered empty state from a retryable request failure
- AND it SHALL NOT replace real data with preview records

#### Scenario: Preview timeline is filtered

- GIVEN the user explicitly entered preview mode
- WHEN the user applies or resets timeline filters or loads another page
- THEN preview data SHALL follow the same query and pagination semantics
- AND preview state SHALL remain isolated from authenticated real mode

### Requirement: M4 Time Review Must Show Real Location And Media

Time review SHALL show real location and media for unlocked records when present.

#### Scenario: User opens unlocked time review

- GIVEN an authenticated user owns an UNLOCKED record with location, image attachments, voice attachments, or cover
- WHEN the user opens 时间回看
- THEN the Mini Program SHALL display those assets as read-only context
- AND image preview and voice playback SHALL use authorized real media access

#### Scenario: Media access fails

- GIVEN media exists but signed URL generation, loading, or playback fails
- WHEN the user views time review
- THEN the Mini Program SHALL show a retryable or understandable failure state
- AND it SHALL NOT silently replace the media with preview data

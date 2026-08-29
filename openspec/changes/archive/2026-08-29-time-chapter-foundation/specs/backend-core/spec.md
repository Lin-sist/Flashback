# Backend Core Spec Delta：time-chapter-foundation（P5.x）

> 规划草案。范围：owner-scoped 时间篇章、成员关系、生命周期、并发、删除和数据所有权集成。

## ADDED Requirements

### Requirement: Time Chapters Must Be Owner Scoped External Containers

The backend SHALL persist a time chapter as an owner-scoped container separate from record content. A chapter SHALL have a non-unique required name, optional user note, ACTIVE or ENDED status, current ended time, version, and timestamps.

#### Scenario: Owner creates a chapter from complete records

- GIVEN an authenticated user owns at least one SAVED, SEALED, or UNLOCKED record
- WHEN the user creates a chapter with a valid name and those record ids
- THEN the backend SHALL create an ACTIVE chapter and its memberships atomically
- AND SHALL NOT copy or modify any record content or context field

#### Scenario: Empty chapter creation is attempted

- GIVEN no eligible record id remains after validation and deduplication
- WHEN chapter creation is requested
- THEN the backend SHALL reject the request atomically
- AND SHALL NOT persist an empty chapter

#### Scenario: Same name is reused

- GIVEN the owner already has a chapter with the requested name
- WHEN another valid chapter is created with that name
- THEN the backend SHALL allow it with a distinct internal id

#### Scenario: Cross-owner access is attempted

- GIVEN a chapter or record belongs to another user
- WHEN the authenticated user queries or mutates it
- THEN the backend SHALL return a safe not-found or owner error
- AND SHALL NOT expose name, note, membership, count, or coverage metadata

### Requirement: Time Chapter Membership Must Be Explicit Atomic And Single Chapter In V1

Each complete record SHALL belong to zero or one time chapter in the first version. Add, remove, and transfer SHALL be explicit owner commands, transactionally atomic, idempotent on retry, and protected from stale confirmation.

#### Scenario: Unassigned records are added

- GIVEN all selected records are owned, complete, and currently unassigned
- AND the target chapter is ACTIVE
- WHEN the owner adds them
- THEN all memberships SHALL be inserted in one transaction
- AND the records themselves SHALL remain unchanged

#### Scenario: Sealed or unlocked record membership changes

- GIVEN an owned record is SEALED or UNLOCKED
- WHEN the owner adds, removes, or transfers its chapter membership
- THEN the backend SHALL allow the external relationship change
- AND SHALL NOT modify content, location, attachments, cover, tags, status, or record timestamps

#### Scenario: Draft membership is attempted

- GIVEN any selected record is DRAFT
- WHEN a create or member command is submitted
- THEN the complete batch SHALL be rejected
- AND no membership or chapter state SHALL change

#### Scenario: Transfer is explicitly confirmed

- GIVEN a selected record belongs to source chapter A
- AND the owner submits target chapter B with the matching source chapter id
- WHEN the backend locks and revalidates the latest ownership and membership
- THEN it SHALL remove the A relationship and add the B relationship atomically

#### Scenario: Transfer confirmation is stale or absent

- GIVEN a selected record currently belongs to a different chapter than the submitted source
- WHEN the command is processed
- THEN the complete batch SHALL be rejected without partial changes
- AND the client SHALL be required to refresh the latest real membership

#### Scenario: Membership command is retried

- GIVEN every requested record is already in the requested final membership state
- WHEN the same owner command is retried
- THEN the backend SHALL return the current state as idempotent success
- AND SHALL NOT create duplicate relationships

### Requirement: Time Chapter Lifecycle Must Be Reversible Without Evaluating Outcome

The backend SHALL support ACTIVE and ENDED chapter states. Ending SHALL only record the owner's current decision that the chapter has paused; reopening SHALL restore the same chapter.

#### Scenario: Active chapter is ended

- GIVEN an owned chapter is ACTIVE
- WHEN the owner ends it with the current version
- THEN status SHALL become ENDED and endedAt SHALL use the injected Clock
- AND member records SHALL remain unchanged

#### Scenario: Ended chapter rejects new members

- GIVEN an owned chapter is ENDED
- WHEN a member add or transfer-in command targets it
- THEN the backend SHALL reject the command
- AND SHALL require the chapter to be reopened first

#### Scenario: Chapter is reopened

- GIVEN an owned chapter is ENDED
- WHEN the owner reopens it with the current version
- THEN the same chapter SHALL become ACTIVE
- AND endedAt SHALL be cleared without copying the chapter or creating lifecycle history

#### Scenario: Metadata is edited in either state

- GIVEN an owned chapter is ACTIVE or ENDED
- WHEN the owner updates valid name or note with the current version
- THEN the backend SHALL persist the new container interpretation
- AND SHALL NOT update member records

### Requirement: Time Chapter Queries Must Use Record Time Without Inventing Life Dates

Chapter list and detail SHALL derive member count and coverage from current memberships and record `createdAt`. The API and UI contract SHALL call the range “片段覆盖时间”.

#### Scenario: Chapter has members

- GIVEN a chapter contains complete records
- WHEN summary or detail is queried
- THEN coverageStartAt SHALL be the minimum member record createdAt
- AND coverageEndAt SHALL be the maximum member record createdAt
- AND member ordering SHALL be stable by createdAt plus record id in requested ASC or DESC order

#### Scenario: Chapter becomes empty passively

- GIVEN the last member is removed, transferred, or its record is deleted
- WHEN the chapter is queried
- THEN the chapter SHALL still exist
- AND memberCount SHALL be 0 and coverage values SHALL be null

#### Scenario: Concurrent mutation uses stale version

- GIVEN a chapter changed after the client loaded it
- WHEN a mutation submits an older expectedVersion
- THEN the backend SHALL reject it without silent overwrite or partial changes
- AND SHALL expose only a safe conflict category requiring refresh

### Requirement: Chapter Deletion And Data Ownership Must Preserve Record Independence

Deleting a chapter SHALL delete only the container and memberships. Existing export, record deletion, and clear-all operations SHALL include chapter relationships without weakening record ownership or seal policy.

#### Scenario: Chapter is deleted

- GIVEN the owner confirms deletion using the current member count and version
- WHEN the backend deletes the chapter
- THEN the chapter and memberships SHALL be removed
- AND every member record SHALL remain complete and owner accessible

#### Scenario: Member record is deleted

- GIVEN a record belongs to a chapter
- WHEN the existing owner-scoped record deletion completes
- THEN its chapter relationship SHALL be removed
- AND the chapter SHALL remain even if empty

#### Scenario: Full export includes chapters

- GIVEN an owner requests an existing data export
- WHEN the export is built
- THEN it SHALL include chapter metadata and member record id relationships in a separate chapter section
- AND SHALL NOT duplicate record content, location, media, or Agent content into chapter files

#### Scenario: Clear all completes

- GIVEN the owner's clear-all operation completes
- WHEN chapter tables are inspected
- THEN no chapter or membership owned by that user SHALL remain
- AND no cross-owner relationship SHALL be affected

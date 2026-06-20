# M4 Real Capability Completion Tasks

## 0. Guardrails

- [x] Read `AGENTS.md`.
- [x] Read `.ai/ACTIVE_TASK.md`.
- [x] Read `openspec/project.md`.
- [x] Read `openspec/specs/backend-core/spec.md`.
- [x] Read `openspec/specs/miniapp-core/spec.md`.
- [x] Read `openspec/specs/v2-product-scope/spec.md`.
- [x] Read `openspec/specs/agent-collaboration/spec.md`.
- [x] Read this M4 OpenSpec change before implementation.
- [x] Read `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md` before backend contract work.
- [x] Do not implement admin portal.
- [x] Do not implement production deployment, monitoring, alerting, incident response, or release operations.
- [x] Do not implement settings page enhancements in M4.
- [x] Do not implement SMS reminders, production notification center, admin template management, or campaign delivery.
- [x] Do not implement speech-to-text, voice transcription, voice AI analysis, complex AI scoring, diagnosis, or dashboards.
- [x] Do not implement social feed, sharing, public discovery, or H5/Web user-side acceptance.
- [x] Do not perform broad backend rewrite.
- [x] Do not perform major frontend visual reconstruction.
- [x] Do not modify package or lockfile files unless required and explicitly justified.
- [x] State a reason before reading extra files outside the current task's direct scope.
- [x] Record all implementation notes, verification evidence, skipped verification reasons, and manual WeChat verification results in `.ai/AGENT_LOG.md`.

## 1. Establish M4 Current Code Facts

- [x] Confirm current AI provider/configuration behavior.
- [x] Confirm all AI call sites and whether each uses mock, fallback, or real provider logic.
- [x] Confirm current record entity/DTO/VO fields for location, attachments, and cover.
- [x] Confirm current record editor location/image/voice entry points.
- [x] Confirm current home card and timeline data sources.
- [x] Confirm current time review detail data source.
- [x] Confirm current preview mode entry and preview-data boundary.
- [x] Confirm current upload/storage dependencies, if any.
- [x] Classify relevant capabilities as `confirmed`, `partial`, `planned`, `out of scope`, or `unknown`.
- [x] Write fact findings to `.ai/AGENT_LOG.md` before implementation.

## 2. Accepted Contract Gate

- [x] Use `backend-contract-decisions.md` accepted defaults unless the user changes them.
- [x] Implement accepted AI provider configuration keys, provider enum values, and model selection behavior.
- [x] Implement the accepted single OpenAI-compatible adapter strategy.
- [x] Implement accepted location API paths and DTO fields.
- [x] Implement accepted attachment upload-token API path and request/response DTO.
- [x] Implement accepted attachment commit/verify API path and request/response DTO.
- [x] Implement accepted attachment delete behavior.
- [x] Implement accepted media signed URL expiry policy.
- [x] Implement accepted cover update API path and DTO.
- [x] Implement accepted frontend-visible upload, verification, and media error states.
- [ ] Record any decision changes in `backend-contract-decisions.md` before implementation.

## 3. Backend Phase: Real AI Provider

- [x] Add configuration for real AI provider, base URL, API key, model, and timeout.
- [x] Keep AI API keys backend-side only.
- [x] Implement DeepSeek or compatible domestic model provider path.
- [x] Support OpenAI-compatible request/response shape where practical.
- [x] Keep mock provider available only for tests or explicit preview/development mode.
- [x] Ensure missing AI configuration returns explicit unavailable behavior.
- [x] Ensure provider failure returns explicit failed/unavailable behavior.
- [x] Ensure AI output never replaces original user content.
- [x] Ensure AI failure does not block record save/seal when AI is not required.
- [x] Add focused backend tests or documented manual verification for success, missing config, provider failure, and privacy-safe logging.

## 4. Backend Phase: Qiniu Storage Foundation

- [x] Add backend-only Qiniu configuration for access key, secret key, bucket, domain/base URL, region/zone if needed, and URL expiry.
- [x] Ensure Qiniu secrets are not present in frontend code or tracked files.
- [x] Implement authenticated upload-token creation.
- [x] Scope upload tokens by user, record, media type, size policy, and generated/approved object key.
- [x] Implement backend object existence verification after upload.
- [x] Verify object size and MIME type where Qiniu metadata supports it.
- [x] Implement private signed URL generation or equivalent private access flow.
- [x] Add focused backend tests or documented manual verification for token creation, object verification, missing object, and signed URL behavior.

## 5. Backend Phase: Attachments

- [x] Add persistence for record attachments using separate table/model or equivalent.
- [x] Track attachment owner, record, type, storage provider, bucket, storage key, MIME type, size, status, sort order, and timestamps.
- [x] Track voice duration where available.
- [x] Track image width/height where available.
- [x] Enforce max 9 images per record.
- [x] Enforce max 9 voice files per record.
- [x] Enforce max 40 MB per file.
- [x] Enforce max 300 MB total attachments per record.
- [x] Allow attachment add/delete only for DRAFT records.
- [x] Reject attachment mutation for SEALED and UNLOCKED records.
- [x] Ensure attachment reads and signed URLs are owner-scoped.
- [x] Keep voice storage as raw voice file only; do not add transcription.
- [x] Update test schema consistently if schema changes.
- [x] Add focused backend tests for ownership, limits, draft delete, sealed/unlocked rejection, and Qiniu verification.

## 6. Backend Phase: Cover

- [x] Add cover reference to record persistence or equivalent.
- [x] Ensure cover is optional.
- [x] Ensure cover must reference an IMAGE attachment.
- [x] Ensure cover attachment belongs to the same record and same owner.
- [x] Reject voice attachments as cover.
- [x] Reject another record's or another user's attachment as cover.
- [x] Allow cover selection/change only for DRAFT records.
- [x] Reject cover mutation for SEALED and UNLOCKED records.
- [x] Define draft behavior when deleting the current cover image.
- [x] Include cover metadata or cover URL in list/timeline/home detail contracts where required.
- [x] Add focused backend tests for cover validation and immutability.

## 7. Backend Phase: Location

- [x] Add persistence for record location using separate table/model or equivalent.
- [x] Support source values `CURRENT_LOCATION`, `MAP_PICKER`, and `MANUAL`.
- [x] Support location name/address fields.
- [x] Support latitude/longitude for current location and map picker where available.
- [x] Allow manual input without coordinates.
- [x] Allow location create/update/delete only for DRAFT records.
- [x] Reject location mutation for SEALED and UNLOCKED records.
- [x] Ensure location reads are owner-scoped.
- [x] Include location in unlocked detail/time review response.
- [x] Add focused backend tests for source validation, ownership, draft mutation, sealed/unlocked rejection, and unlocked detail display.

## 8. Frontend Phase: Real AI Connection

- [x] Update AI service calls to use backend real-provider contract.
- [x] Show explicit unavailable/failed state when AI is not configured or provider call fails.
- [x] Do not show mock AI output in authenticated real mode.
- [x] Keep original content visible and unchanged after AI organization.
- [ ] Verify real AI flow with configured provider where available.

## 9. Frontend Phase: Location

- [x] Replace location placeholder/toast with real location controls.
- [x] Support current location selection.
- [x] Support map picker selection.
- [x] Support manual input.
- [x] Support draft edit/delete.
- [x] Show read-only location after seal/unlock where applicable.
- [x] Show location in time review after unlock.
- [x] Handle permission denial and unavailable location services without blocking record editing.
- [ ] Verify Mini Program location behavior manually where automation is not practical.

## 10. Frontend Phase: Images, Voice, and Cover

- [x] Replace image placeholder/toast with real image selection.
- [x] Compress images by default before upload.
- [x] Upload images through Qiniu token flow.
- [x] Preview uploaded images.
- [x] Delete draft images.
- [x] Replace voice placeholder/toast with real voice recording.
- [x] Upload raw voice files through Qiniu token flow.
- [x] Play uploaded voice files.
- [x] Support draft voice re-record/delete.
- [x] Enforce or pre-check max 9 images, max 9 voice files, 40 MB per file, and 300 MB per record.
- [ ] Add cover selection UI from current record image attachments.
- [ ] Prevent cover selection when no image attachment exists.
- [ ] Show cover on timeline/home cards when available.
- [ ] Show attachments as read-only after seal/unlock.
- [ ] Verify media flow manually in WeChat Developer Tools where automation is not practical.

## 11. Frontend Phase: Mock Boundary and Real Data Surfaces

- [ ] Audit preview/mock data usage in frontend services and pages touched by M4.
- [ ] Keep preview data only behind explicit preview mode.
- [ ] Ensure authenticated real users use backend-backed record, review, cover, location, and attachment data.
- [ ] Replace hard-coded home review countdown/card data with backend-backed real-mode data.
- [ ] Ensure time review uses backend-backed detail, attachments, location, and cover in real mode.
- [ ] Add safe empty/loading/error states for real-mode data failures.
- [ ] Verify preview mode still works after mock-boundary changes.

## 12. Integration and Verification

- [x] Run focused backend tests where practical.
- [x] Run full backend test suite when feasible.
- [x] Run frontend type-check when feasible.
- [x] Run Mini Program build when feasible.
- [x] Verify no tracked AI or Qiniu secrets are committed.
- [ ] Verify real AI configured success path where provider credentials are available.
- [x] Verify AI missing-config/failure path.
- [ ] Verify Qiniu upload, object verification, signed URL, image preview, and voice playback.
- [x] Verify attachment limit errors and record total-size limit errors.
- [x] Verify sealed/unlocked records reject location, attachment, and cover mutation.
- [ ] Verify timeline/home cover display.
- [ ] Verify unlocked time review displays location, image, voice, and M3 reflection data.
- [ ] Verify preview mode remains explicitly isolated and functional.
- [x] Record verification evidence and skipped verification reasons in `.ai/AGENT_LOG.md`.

## 13. Final Review

- [ ] Confirm M4 did not implement settings page work.
- [ ] Confirm M4 did not add admin, production deployment, monitoring, SMS, notification center, campaign delivery, social feed, or H5/Web acceptance scope.
- [ ] Confirm AI is real-provider-backed in authenticated real mode.
- [ ] Confirm missing real integrations fail explicitly and do not fake success.
- [ ] Confirm Qiniu bucket usage assumes private access and backend-signed URLs.
- [ ] Confirm Qiniu AK/SK and AI keys are backend-only and not tracked.
- [ ] Confirm location, attachments, and cover are immutable after seal.
- [ ] Confirm cover comes only from same-record image attachments.
- [ ] Confirm voice is stored as raw audio only with no transcription.
- [ ] Confirm V2.0 visible naming remains "我的记录", "时光轴", and "时间回看".
- [ ] Include modified files, what changed, verification result, skipped verification reason, `git diff --stat`, scope safety check, and remaining risks in final handoff.

# M3 Demo Core Flow Hardening Tasks

## 0. Guardrails

- [x] Read `AGENTS.md`.
- [x] Read `.ai/ACTIVE_TASK.md`.
- [x] Read `openspec/project.md`.
- [x] Read `openspec/specs/backend-core/spec.md`.
- [x] Read this M3 OpenSpec change before implementation.
- [x] Read `openspec/changes/m3-demo-core-flow-hardening/backend-contract-decisions.md` before backend contract work.
- [x] Do not implement admin portal.
- [x] Do not implement production deployment, monitoring, alerting, or incident response.
- [x] Do not implement SMS reminders.
- [x] Do not implement a production notification center.
- [x] Do not implement admin template management or campaign delivery.
- [x] Do not implement complex AI growth analysis, scoring, diagnosis, or dashboards.
- [x] Do not implement real MAP / IMAGE / VOICE capability.
- [x] Do not perform broad frontend visual reconstruction.
- [x] Do not perform broad backend rewrite.
- [x] Do not remove account/password login.
- [x] Do not modify package or lockfile files unless required and explicitly justified.
- [x] State a reason before reading extra files outside the current task's direct scope.
- [x] Record all implementation notes, verification evidence, skipped verification reasons, and manual WeChat verification results in `.ai/AGENT_LOG.md`.

## 1. Establish M3 Backend Facts

- [x] Confirm actual current backend auth flow.
- [x] Confirm current user model and OpenID support.
- [x] Confirm current record DTO/entity/VO fields.
- [x] Confirm current AI prompt and summary APIs.
- [x] Confirm current unlock scheduler behavior.
- [x] Confirm current reminder/outbox/log foundation inherited from M2.
- [x] Confirm current backend modules for records, replies, tags, timeline, AI, reminders, and users.
- [x] Document confirmed, partial, planned, unknown, and out-of-scope capabilities before implementation.
- [x] Write fact findings to `.ai/AGENT_LOG.md`.

## 2. Backend Contract Confirmation Gate

- [x] Do not implement new or changed M3 backend API contracts until user confirmation is recorded.
- [x] Update `backend-contract-decisions.md` from `Pending` to `Accepted` or `Deferred` after user confirmation.
- [x] Confirm WeChat login endpoint path and request/response DTO.
- [x] Confirm whether local account binding to WeChat is included in M3 or deferred.
- [x] Confirm record reflection field names exposed to frontend.
- [x] Confirm whether "你当时以为" is stored on record, generated on demand, or both.
- [x] Confirm "后来其实" endpoint path and update semantics.
- [x] Confirm life node enum names and custom label validation behavior.
- [x] Confirm reminder status values exposed to frontend.
- [x] Confirm stage summary endpoint path.
- [x] Confirm whether stage summaries are persisted or generated on demand.
- [x] Record confirmed contract decisions in the M3 OpenSpec documents before implementation.

## 3. Backend Schema and Contract Planning

- [x] Add or update schema support for "你当时以为" only after contract confirmation.
- [x] Add or update schema support for "后来其实" only after contract confirmation.
- [x] Add or update schema support for life node enum.
- [x] Add or update schema support for custom life node label when enum is OTHER.
- [x] Do not add stage summary persistence in M3; summaries are generated on demand.
- [ ] Add or update schema support for reminder configuration/status if M2 foundation is insufficient.
- [x] Keep schema changes narrow.
- [x] Document that demo database rebuild is allowed.
- [x] Update test schema consistently when backend schema changes.
- [x] Record schema decisions and rebuild expectations in `.ai/AGENT_LOG.md`.

## 4. Backend Phase: Authentication

- [x] Preserve existing account/password register and login behavior.
- [x] Add `POST /api/auth/wechat-login`.
- [x] Ensure backend obtains OpenID through WeChat code2session.
- [x] Reject or fail explicitly when WeChat configuration is missing.
- [x] Do not trust client-supplied OpenID as login identity.
- [x] Reuse existing JWT response model where practical.
- [x] Support lookup or creation of user by OpenID.
- [x] Defer account/password to WeChat binding.
- [x] Return user info that makes WeChat binding/login status understandable.
- [x] Add backend tests or manual verification for account/password login.
- [x] Add backend tests or manual verification for WeChat login configured and not-configured paths.

## 5. Backend Phase: Record Reflection Fields

- [x] Extend create/update DTOs or equivalent contracts for M3 reflection fields after contract confirmation.
- [x] Preserve user original content.
- [ ] Add user-triggered AI organization for `beliefThen`.
- [ ] Ensure AI output does not replace original content.
- [ ] Ensure AI failure does not block record save or seal when non-AI validation passes.
- [x] Enforce that "后来其实" cannot be created or updated before unlock.
- [x] Add `PUT /api/records/{recordId}/later-reflection` with request `{ "realityLater": "string" }`.
- [x] Enforce that "后来其实" can be submitted at most 2 times after unlock by the owner.
- [x] Reject additional "后来其实" updates after the 2-submit limit is exhausted.
- [x] Ensure cross-user access to reflection fields is rejected.
- [x] Add tests for before-unlock, after-unlock, cross-user, and edit-limit "后来其实" behavior.

## 6. Backend Phase: Life Node Records

- [x] Define supported life node enum values:
  - GRADUATION
  - WORK
  - MOVE
  - RELATIONSHIP
  - HEALTH
  - FAMILY
  - TURNING_POINT
  - OTHER
- [ ] Map enum values to visible labels:
  - 毕业
  - 工作
  - 搬家
  - 关系
  - 健康
  - 家庭
  - 转折
  - 其他
- [x] Allow custom life node label only when enum is OTHER.
- [x] Reject custom life node labels on non-OTHER values with validation failure.
- [ ] Ensure NODE_RECORD can be filtered or displayed without affecting other record types.
- [ ] Add tests or manual verification for fixed enum and OTHER custom label.

## 7. Backend Phase: Unlock Reminder Delivery

- [ ] Confirm reminder authorization timing in the frontend seal flow.
- [ ] Store reminder intent or authorization result where required.
- [ ] Implement or complete WeChat subscription-message send adapter.
- [ ] Make template ID configuration-driven.
- [ ] Treat missing template ID as explicit `not_configured` behavior.
- [ ] Ensure `not_configured` does not block seal.
- [ ] Ensure `not_configured` does not block unlock.
- [ ] Ensure send failure does not block unlock.
- [ ] Ensure successful send is idempotent by record and template type.
- [ ] Record `DENIED` when the user refuses subscription authorization.
- [ ] Ensure logs and reminder records do not include record content, auth tokens, or unnecessary sensitive identifiers.
- [ ] Add tests or manual verification for success, failure, duplicate-send, and not-configured behavior.
- [ ] Record that real delivery must be manually verified after template IDs are configured.

## 8. Backend Phase: Stage Summary

- [ ] Add `POST /api/stage-summaries/generate`.
- [ ] Ensure stage summary is scoped to the authenticated user.
- [ ] Use lightweight statistics and AI organization only.
- [ ] Ensure stage summary does not become a forced lifecycle step.
- [ ] Ensure AI failure returns safe fallback or explicit unavailable state.
- [ ] Do not persist stage summaries in M3.
- [ ] Add tests or manual verification for manual generation and ownership safety.

## 9. Frontend Phase: Flow Completion

- [ ] Start this phase only after backend contracts are confirmed.
- [ ] Update login page to support WeChat login and account/password login.
- [ ] Keep preview mode clearly separate from real auth.
- [ ] Update auth service to call WeChat login endpoint.
- [ ] Update record editor to support user-triggered AI organization for "你当时以为".
- [ ] Update record editor to support life node enum and OTHER custom label.
- [ ] Update seal flow to request subscription authorization after successful seal.
- [ ] Ensure subscription refusal does not undo seal UI state.
- [ ] Update time review to display "你当时以为".
- [ ] Update time review to allow "后来其实" only after unlock and only while the 2-submit limit allows modification.
- [ ] Hide the "修改" action after the second "后来其实" submission.
- [ ] Add manual stage summary entry point in Personal Center only.
- [ ] Ensure visible naming uses "我的记录", "时光轴", and "时间回看".
- [ ] Avoid major visual redesign unless needed to complete the flow.

## 10. Integration and Verification Phase

- [ ] Run focused backend tests where practical.
- [ ] Run full backend test suite when feasible.
- [ ] Run frontend type-check when feasible.
- [ ] Run Mini Program build when feasible.
- [ ] Manually verify account/password login.
- [ ] Manually verify WeChat login in WeChat Developer Tools when configuration is available.
- [ ] Manually verify missing WeChat template ID behavior.
- [ ] Manually verify real reminder delivery after template IDs are configured.
- [ ] Manually verify create -> user-triggered AI organize -> seal -> unlock -> reminder attempt -> time review -> "后来其实" first submit -> "后来其实" second submit -> no more modify action -> stage summary.
- [ ] Record verification evidence and skipped verification reasons in `.ai/AGENT_LOG.md`.

## 11. Final Review

- [ ] Confirm account/password login was not removed.
- [ ] Confirm WeChat login does not trust client-supplied OpenID.
- [ ] Confirm original record content remains preserved.
- [ ] Confirm "后来其实" is after-unlock only.
- [ ] Confirm "后来其实" cannot be submitted more than 2 times.
- [ ] Confirm reminders are idempotent and non-blocking.
- [ ] Confirm missing template ID does not fake success.
- [ ] Confirm real delivery verification is either recorded or explicitly pending template ID configuration.
- [ ] Confirm stage summary is manual.
- [ ] Confirm stage summary is generated on demand and not persisted.
- [ ] Confirm admin, SMS, production notification center, deployment, monitoring, complex AI, and broad redesign stayed out of scope.
- [ ] Include modified files, what changed, verification result, skipped verification reason, `git diff --stat`, scope safety check, and remaining risks in final handoff.

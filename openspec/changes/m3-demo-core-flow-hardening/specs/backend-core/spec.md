# Backend Core Spec Delta

## ADDED Requirements

### Requirement: M3 Backend Contracts Must Be Confirmed Before Implementation

The backend SHALL NOT finalize new or changed M3 API contracts from agent assumption when more than one reasonable contract exists.

#### Scenario: Contract decision is pending

- GIVEN an endpoint path, DTO field, enum name, persistence model, or frontend-visible status is not explicitly accepted
- WHEN an Agent prepares M3 backend implementation
- THEN the Agent SHALL ask the user for confirmation
- AND it SHALL record the accepted decision in M3 OpenSpec before implementation

#### Scenario: Existing contract is reused

- GIVEN an existing endpoint or DTO can satisfy M3 behavior without changing the external contract
- WHEN an Agent reuses it
- THEN the Agent SHALL document the reuse decision in `.ai/AGENT_LOG.md`

#### Scenario: M3 contract decisions are accepted

- GIVEN the user has confirmed the M3 backend contract decisions
- WHEN an Agent implements M3 backend work
- THEN it SHALL follow `openspec/changes/m3-demo-core-flow-hardening/backend-contract-decisions.md`
- AND it SHALL NOT replace accepted decisions with a different endpoint, field, enum, or persistence contract without a new OpenSpec update

### Requirement: M3 Authentication Must Support Account Password And WeChat Login

The backend SHALL preserve account/password login and add real WeChat code login for M3.

The accepted WeChat login endpoint is `POST /api/auth/wechat-login`.

#### Scenario: Account password login remains supported

- GIVEN account/password login existed before M3
- WHEN M3 authentication work is implemented
- THEN account/password login SHALL continue to work if credentials are valid

#### Scenario: WeChat code login exchanges trusted identity

- GIVEN the backend receives a Mini Program login code
- WHEN WeChat configuration is available
- THEN the backend SHALL exchange the code through WeChat code2session or equivalent trusted server-side API
- AND it SHALL use the returned OpenID or session identity as trusted identity data
- AND it SHALL NOT trust a client-supplied OpenID as login proof

#### Scenario: WeChat login is not configured

- GIVEN required WeChat login configuration is absent
- WHEN WeChat login is attempted
- THEN the backend SHALL return an explicit not-configured or unavailable response
- AND it SHALL NOT issue a fake successful WeChat login

#### Scenario: Account binding is deferred

- GIVEN account/password login and WeChat login both exist
- WHEN M3 authentication work is implemented
- THEN explicit binding between an existing account/password user and a WeChat identity SHALL remain deferred
- AND account merge behavior SHALL NOT be required for M3 acceptance

### Requirement: M3 Reflection Fields Must Preserve Original Content

The backend SHALL support M3 reflection fields without replacing the user's original record content.

#### Scenario: AI organizes "你当时以为"

- GIVEN a user has written original record content
- WHEN the user actively triggers AI organization and AI organization succeeds
- THEN the backend MAY store or return `beliefThen`
- AND the original content SHALL remain unchanged

#### Scenario: AI organization fails

- GIVEN AI organization is unavailable or fails
- WHEN the user saves or seals a record and non-AI validation passes
- THEN the backend SHALL allow the core record operation to continue
- AND it SHALL return safe fallback or unavailable behavior for AI output

### Requirement: Later Reflection Must Be After Unlock Only

The backend SHALL allow "后来其实" only for records that are UNLOCKED and owned by the authenticated user.

The accepted field name is `realityLater`. The accepted endpoint is `PUT /api/records/{recordId}/later-reflection`.

#### Scenario: Owner writes later reflection after unlock

- GIVEN a record is UNLOCKED
- AND the authenticated user owns the record
- AND the user has not exhausted the 2-submit limit
- WHEN the user creates or updates `realityLater`
- THEN the backend SHALL persist the value or return it through the supported contract
- AND the backend SHALL increment or otherwise enforce the later-reflection submit count

#### Scenario: User writes later reflection before unlock

- GIVEN a record is DRAFT or SEALED
- WHEN a user attempts to create or update "后来其实"
- THEN the backend SHALL reject the operation
- AND the record SHALL remain unchanged

#### Scenario: User exceeds later reflection edit limit

- GIVEN a record is UNLOCKED
- AND the authenticated user owns the record
- AND the user has already submitted `realityLater` 2 times
- WHEN the user attempts to update `realityLater` again
- THEN the backend SHALL reject the operation
- AND the existing `realityLater` SHALL remain unchanged

#### Scenario: Cross-user later reflection is rejected

- GIVEN a record belongs to another user
- WHEN the authenticated user attempts to read or mutate "后来其实"
- THEN the backend SHALL reject the operation or return a safe not-found response
- AND private content SHALL NOT be exposed

### Requirement: Life Node Values Must Be Constrained

The backend SHALL support a constrained M3 life node model.

#### Scenario: Supported life node enum is submitted

- WHEN a NODE_RECORD uses GRADUATION, WORK, MOVE, RELATIONSHIP, HEALTH, FAMILY, TURNING_POINT, or OTHER
- THEN the backend SHALL accept the enum if all other validation passes

#### Scenario: Other life node uses custom label

- GIVEN the life node enum is OTHER
- WHEN the user provides a custom life node label
- THEN the backend SHALL accept the label if validation passes

#### Scenario: Custom label is submitted for non-other life node

- GIVEN the life node enum is not OTHER
- WHEN a custom life node label is submitted
- THEN the backend SHALL reject the request with validation failure

### Requirement: M3 Unlock Reminder Delivery Must Be Real But Non-Blocking

The backend SHALL support a real WeChat subscription-message delivery path for unlock reminders while remaining demo-scoped.

Accepted reminder statuses are `REQUESTED`, `AUTHORIZED`, `DENIED`, `NOT_CONFIGURED`, `SEND_PENDING`, `SEND_SUCCESS`, `SEND_FAILED`, and `SKIPPED_NO_OPENID`.

#### Scenario: Template ID is configured

- GIVEN a record becomes UNLOCKED
- AND reminder authorization and template configuration allow delivery
- WHEN the reminder send path runs
- THEN the backend SHALL attempt a real WeChat subscription-message send
- AND successful send SHALL be recorded idempotently by record and template type

#### Scenario: Template ID is missing

- GIVEN the WeChat subscription template ID is not configured
- WHEN a reminder send would otherwise occur
- THEN the backend SHALL record or expose `NOT_CONFIGURED`
- AND it SHALL NOT record fake send success
- AND it SHALL NOT roll back the unlock transition

#### Scenario: User denies subscription authorization

- GIVEN the frontend receives user refusal for subscription authorization
- WHEN the refusal is reported to the backend
- THEN the backend SHALL record `DENIED` where reminder status is persisted

#### Scenario: Reminder send fails

- GIVEN the unlock transition succeeds
- WHEN reminder delivery fails
- THEN the backend SHALL record the failure where logging exists
- AND the unlock transition SHALL remain successful
- AND the unlock task SHALL continue processing other eligible records

#### Scenario: Reminder path is retried

- GIVEN a successful unlock reminder has already been recorded for a record and template type
- WHEN the reminder path runs again
- THEN the backend SHALL NOT send another successful duplicate message

### Requirement: Stage Summary Must Be User Scoped And Manual

The backend SHALL generate M3 stage summaries only through user manual action.

The accepted endpoint is `POST /api/stage-summaries/generate`. M3 stage summaries SHALL be generated on demand and SHALL NOT be persisted.

#### Scenario: User manually requests stage summary

- GIVEN an authenticated user requests stage summary generation
- WHEN eligible records exist
- THEN the backend SHALL use only that user's data
- AND it MAY include lightweight statistics and AI-organized text
- AND it SHALL return the generated summary directly without saving summary history

#### Scenario: Stage summary AI fails

- GIVEN AI stage summary generation fails
- WHEN the user requested a summary
- THEN the backend SHALL return a safe fallback or explicit unavailable state
- AND it SHALL NOT mutate unrelated records or lifecycle state

#### Scenario: Cross-user summary data is prevented

- GIVEN multiple users have records
- WHEN one user requests a stage summary
- THEN another user's records, reflections, replies, and private content SHALL NOT be included

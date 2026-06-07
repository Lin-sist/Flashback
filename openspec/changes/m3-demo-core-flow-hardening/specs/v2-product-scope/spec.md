# V2 Product Scope Spec Delta

## ADDED Requirements

### Requirement: M3 Must Remain Demo Core Flow Hardening

M3 SHALL be treated as demo core flow hardening, not production launch.

#### Scenario: Agent implements M3 work

- WHEN an Agent starts M3 implementation
- THEN it SHALL prioritize complete WeChat Mini Program user-side flow
- AND it SHALL NOT treat production release readiness as the acceptance standard

### Requirement: M3 May Use Configuration Placeholders For WeChat Templates

M3 MAY implement the real subscription-message send path before actual WeChat template IDs are available.

#### Scenario: WeChat template ID is unavailable

- WHEN the template ID is missing
- THEN the system SHALL use explicit not-configured behavior
- AND this SHALL be considered acceptable for M3 demo hardening if the real send path works when configuration is supplied

### Requirement: M3 May Rebuild Demo Database

M3 MAY rely on demo database rebuilds for schema changes.

#### Scenario: Schema changes are needed for M3

- WHEN M3 adds reflection, life node, reminder, or stage summary fields
- THEN implementation MAY update schema files and test schema directly
- AND a full migration framework SHALL NOT be required in M3

### Requirement: M3 Must Not Add Admin Or Production Modules

M3 SHALL keep admin and production-only modules outside scope.

#### Scenario: Agent encounters admin or production work

- WHEN work suggests admin portal, production deployment, monitoring, alerting, SMS, campaign delivery, or production notification center
- THEN the Agent SHALL defer it to a future OpenSpec change
- AND it SHALL NOT implement it as part of M3


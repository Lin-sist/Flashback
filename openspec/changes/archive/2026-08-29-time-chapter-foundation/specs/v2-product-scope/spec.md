# V2 Product Scope Spec Delta：time-chapter-foundation（P5.x）

> 规划草案。范围：时间篇章最小产品边界、证据豁免与后续优化边界。

## ADDED Requirements

### Requirement: V2 May Add User Defined Time Chapters After Explicit Product Governance Waiver

V2 MAY implement the minimum time chapter foundation after the product owner explicitly waives the unavailable target-user evidence prerequisite. The waiver SHALL NOT be represented as positive user research.

#### Scenario: P5.x enters planning without target users

- GIVEN E1 remains INCONCLUSIVE with zero valid target participants
- AND the product owner explicitly chooses to complete the minimum product before real-user optimization
- WHEN P5.x planning begins
- THEN the project MAY create a Type C change under the normal planning and implementation gates
- AND all artifacts SHALL preserve the missing-user-evidence boundary

#### Scenario: Engineering validation passes

- GIVEN automated, MySQL, or WeChat Developer Tools validation passes
- WHEN P5.x status is reported
- THEN the project MAY claim the verified engineering capability
- AND SHALL NOT claim that users naturally understand, need, or prefer time chapters

### Requirement: V2 Time Chapters Must Remain Optional Non Evaluative Life Containers

The first time chapter capability SHALL be optional, user-defined, reversible, and independent from record creation. It SHALL NOT become a productivity, progress, outcome, or AI classification system.

#### Scenario: User never creates a chapter

- GIVEN a user only saves independent records
- WHEN the user continues using Flashback
- THEN the complete save, browse, seal, unlock, review, export, and delete paths SHALL remain usable
- AND no chapter selection SHALL be required

#### Scenario: Chapter is used

- GIVEN a user recognizes that existing moments belong to the same part of life
- WHEN the user creates and manages a chapter
- THEN the container MAY be named, ended, reopened, edited, and deleted by that user
- AND SHALL NOT assign goals, progress, completion, success, failure, diagnosis, or predicted life-stage meaning

#### Scenario: Future optimization is considered

- GIVEN the minimum product later receives real usage evidence
- WHEN multi-chapter membership, automation, cover, ordering, sharing, or other expansion is proposed
- THEN each expansion SHALL require a separate OpenSpec decision and evidence review
- AND SHALL NOT be silently pre-enabled by P5.x

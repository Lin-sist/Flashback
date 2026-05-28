# Active Task

## Task

Fix two frontend issues:

1. New Record page cannot edit title; it currently only edits content.
2. Register page still shows nickname field; remove nickname input and make register form match login form structure.

## Allowed Files

Prefer only:

- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/pages/login/index.vue`

If necessary, frontend-only form type / validation / style files may be read or modified, but the Agent must explain why.

## Forbidden Files

- backend
- database
- schema
- business rules
- record state transition
- AI
- subscription-message
- real MAP / IMAGE / VOICE implementation
- package / lockfile
- unrelated pages

## Requirements

### New Record Title Input

- Check whether existing frontend data model already has `title`.
- If title exists, bind a title input in the New Record page and ensure it is included in submit/save payload.
- Do not modify backend or schema.
- If title does not exist anywhere in the current frontend/API contract, stop and report instead of inventing backend changes.
- Do not break content input.

### Register Nickname Removal

- Remove nickname input from register UI.
- Remove nickname required validation.
- Make register form visually consistent with login form.
- If backend requires nickname, use the smallest frontend-compatible fallback and report it.
- Do not change login flow.
- Do not rewrite the whole login page.

## Output Required

- modified files
- implementation summary
- validation result
- `git diff --stat`
- scope safety check
- unresolved questions

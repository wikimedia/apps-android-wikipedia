---
name: code-review
description: Review pull requests for correctness, regressions, and architecture problems. Use when conducting a pull request code review.
---

First understand what the pull request is trying to change and how it should behave.

Review the changes for real problems that are relevant to the pull request:
- Behavior that does not work as intended.
- Existing behavior that could be broken by the change.
- Changes that go against established codebase patterns and cause a real issue.

Do not review changed lines in isolation. When existing code is modified, check the related code and callers as needed to determine whether the change introduces issues or unintentionally changes existing behavior.

Only report an issue if:
- The PR introduces the problem or makes an existing problem worse.
- There is a clear scenario where it can occur.
- It has a meaningful impact.
- The changed code is responsible for the issue.
- The issue is relevant and actionable.

Do not report:
- Personal style preferences.
- A problem without a clear example of how it could happen.
- Existing problems that are unrelated to the pull request.
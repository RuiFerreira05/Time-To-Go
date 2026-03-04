---
trigger: model_decision
description: This rule must be applied whenever any change is made to the codebase (new files, modified files, or deleted files).
---

## Rule

After completing any code change, you **must** commit the changes to the local Git repository. Follow these steps:

1. **Stage the changes**: Use `git add` to stage all relevant files that were modified, created, or deleted.
2. **Write a clear commit message**: Every commit message **must** start with the `(AGENT)` prefix, followed by a conventional commit type (e.g., `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). The message must be concise and accurately describe what was changed and why.
3. **Commit locally**: Use `git commit` to commit the staged changes. Do **not** push to a remote unless explicitly asked by the user.

### Example

```bash
git add -A
git commit -m "(AGENT) feat: add user login screen with email validation"
```

### Important Notes

- **Do not** batch unrelated changes into a single commit. If multiple independent changes are made, create separate commits for each logical unit of work.
- **Do not** push to remote repositories unless the user explicitly requests it.
- If a change spans multiple files that all relate to the same feature or fix, they can be grouped into a single commit.

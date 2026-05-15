# Copilot Workspace Instructions

## Session Bootstrap Rule

At the start of every new conversation in this workspace, read and use the project board files in `agent-meta/` before proposing changes:

- `agent-meta/MANIFESTO.md`
- `agent-meta/MIGRATION_TARGET_FEATURES.md`

Expected behavior:
- Treat these files as the primary continuity source for goals, architecture direction, and migration parity.
- Summarize the active priorities from these files before writing code.
- If a request conflicts with these files, call out the conflict and ask for direction.
- Keep implementation choices aligned with performance goals for large-scale soldier battles.

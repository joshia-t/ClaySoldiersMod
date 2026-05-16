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

## Fabric API Retrieval-First Rule

Before generating or modifying Fabric API calls, query the local Fabric API index first.

Index tool:
- `python tools/api_index/fabric_api_index.py query --version 0.149.0+26.1.2 --q "<query>"`

If MCP server is configured, prefer calling tool `query_fabric_api` instead of shelling out.

Expected behavior:
- Retrieve likely classes and members from the local index before proposing code.
- Prefer symbols returned by the local index over memory/guessing.
- If no strong match appears, state uncertainty and run a narrower follow-up query.
- Keep injected API context compact (top matches only) to preserve token budget.

## Fabric Tutorial Retrieval-First Rule

Before answering Fabric "how do I..." implementation questions, query the local tutorial index first.

Tutorial index tool:
- `python tools/api_index/fabric_docs_tutorial_index.py query --q "<query>"`

If MCP server is configured, prefer calling tool `query_fabric_tutorials` or `query_fabric_tutorials_compact`.

Expected behavior:
- Retrieve the most relevant tutorial pages/snippets before proposing step-by-step guidance.
- Prefer guidance that is anchored in retrieved docs pages rather than memory.
- Keep injected tutorial context compact and task-focused.

# Fabric API Local Index

This utility mirrors Fabric Javadoc search indexes and builds a local SQLite + FTS index for fast retrieval.

## Why

- Prevent LLM hallucination by retrieving exact local symbols/signatures.
- Keep prompt size low by injecting only top matching API items.
- Pin retrieval to a specific Fabric API version.

## Commands

Run from repository root.

```powershell
python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2
```

If a prior database is damaged or stale, force a clean rebuild:

```powershell
python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2 --recreate
```

Optional: also mirror class HTML pages for full offline browsing.

```powershell
python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2 --download-pages
```

Query top hits:

```powershell
python tools/api_index/fabric_api_index.py query --version 0.149.0+26.1.2 --q "EntityType Builder createMob"
```

Query as JSON:

```powershell
python tools/api_index/fabric_api_index.py query --version 0.149.0+26.1.2 --q "ServerPlayNetworking registerReceiver" --json
```

## Storage Layout

- `API Documents/fabric-api/<version>/raw/`: downloaded Javadoc search-index JS files.
- `API Documents/fabric-api/<version>/pages/`: optional mirrored class pages.
- `API Documents/index/fabric_api_<version>.sqlite`: local retrieval database.

## Integrating with Copilot Chat

Recommended retrieval-first flow:

1. Run a query for symbols needed by your task.
2. Paste only top 5-12 lines into chat context.
3. Ask Copilot to generate code using only retrieved symbols.
4. If a symbol is missing, query again with package or class keywords.

This keeps token usage low while staying accurate.

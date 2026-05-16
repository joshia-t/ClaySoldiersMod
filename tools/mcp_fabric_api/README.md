# Fabric API MCP Server

This MCP server exposes local Fabric API retrieval tools to Copilot Chat.

## Tools exposed

- `query_fabric_api`: query local SQLite index for classes/members.
- `query_fabric_api_compact`: low-token prompt-ready context block (recommended for generation prompts).
- `sync_fabric_api_index`: refresh local index from Fabric Javadocs.
- `sync_fabric_tutorial_index`: crawl and rebuild local Fabric tutorial/how-to index.
- `query_fabric_tutorials`: query local tutorial/how-to pages.
- `query_fabric_tutorials_compact`: low-token prompt-ready tutorial context block.

## 1) Install dependency

From repository root:

```powershell
python -m pip install -r tools/mcp_fabric_api/requirements.txt
```

## 2) Ensure local index exists

```powershell
python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2 --recreate
python tools/api_index/fabric_docs_tutorial_index.py sync --seed-url https://docs.fabricmc.net/develop/getting-started/creating-a-project --include-prefix /develop/ --max-pages 250 --max-depth 5 --recreate
```

## 3) Add MCP server to Copilot Chat (Tools)

Add this server entry in your VS Code MCP config.

```json
{
  "servers": {
    "fabric-api-index": {
      "type": "stdio",
      "command": "python",
      "args": [
        "tools/mcp_fabric_api/server.py"
      ],
      "cwd": "${workspaceFolder}"
    }
  }
}
```

After saving config, reload VS Code or restart MCP servers.

## 4) Use in chat

Prompt example:

- "Use tool query_fabric_api for version 0.149.0+26.1.2 and find ServerPlayNetworking registerReceiver signatures."
- "Use tool query_fabric_api_compact with query 'EntityType Builder createMob' and include only compact context."
- "Use tool query_fabric_tutorials for query 'custom item group' and return top 6 how-to pages."
- "Use tool query_fabric_tutorials_compact for query 'creating a project gradle' and include compact context."

## Notes

- This is stdio MCP, so VS Code launches it on demand. No always-on daemon required.
- If Python is not on PATH, replace `command` with the full Python executable path.

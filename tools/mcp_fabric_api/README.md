# Fabric API MCP Server

This MCP server exposes local Fabric API retrieval tools to Copilot Chat.

## Tools exposed

- `query_fabric_api`: query local SQLite index for classes/members.
- `query_fabric_api_compact`: low-token prompt-ready context block (recommended for generation prompts).
- `sync_fabric_api_index`: refresh local index from Fabric Javadocs.

## 1) Install dependency

From repository root:

```powershell
python -m pip install -r tools/mcp_fabric_api/requirements.txt
```

## 2) Ensure local index exists

```powershell
python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2 --recreate
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

## Notes

- This is stdio MCP, so VS Code launches it on demand. No always-on daemon required.
- If Python is not on PATH, replace `command` with the full Python executable path.

import asyncio
from tools.mcp_fabric_api.server import mcp

async def check():
    tools = await mcp.list_tools()
    for t in tools:
        print(f"- {t.name}")

asyncio.run(check())

#!/usr/bin/env python3
import sys
sys.path.insert(0, '.')

import asyncio
from tools.mcp_fabric_api.server import mcp

async def main():
    tools = await mcp.list_tools()
    print(f"Total tools: {len(tools)}")
    for t in tools:
        print(f"  - {t.name}")

if __name__ == "__main__":
    asyncio.run(main())

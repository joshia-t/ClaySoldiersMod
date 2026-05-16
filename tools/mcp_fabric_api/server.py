#!/usr/bin/env python3
"""MCP server exposing Fabric API index queries as a tool.

This server is intended to be launched by Copilot Chat as an MCP stdio tool.
"""

from __future__ import annotations

import json
import pathlib
import subprocess
import sys
from typing import Any

try:
    from mcp.server.fastmcp import FastMCP
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Missing dependency 'mcp'. Install with: python -m pip install -r tools/mcp_fabric_api/requirements.txt"
    ) from exc


ROOT = pathlib.Path(__file__).resolve().parents[2]
INDEX_SCRIPT = ROOT / "tools" / "api_index" / "fabric_api_index.py"
TUTORIAL_INDEX_SCRIPT = ROOT / "tools" / "api_index" / "fabric_docs_tutorial_index.py"
DEFAULT_VERSION = "0.149.0+26.1.2"
QUERY_TIMEOUT_SECONDS = 30
DEFAULT_TUTORIAL_SEED_URL = "https://docs.fabricmc.net/develop/getting-started/creating-a-project"
DEFAULT_TUTORIAL_INCLUDE_PREFIX = "/develop/"

mcp = FastMCP("fabric-api-index")


def _format_type_hit(entry: dict[str, Any], include_urls: bool) -> str:
    package_name = (entry.get("package_name") or "").strip()
    class_name = (entry.get("class_name") or "").strip()
    target_url = (entry.get("target_url") or "").strip()

    fqcn = f"{package_name}.{class_name}" if package_name else class_name
    if include_urls and target_url:
        return f"- TYPE {fqcn} | {target_url}"
    return f"- TYPE {fqcn}"


def _format_member_hit(entry: dict[str, Any], include_urls: bool) -> str:
    package_name = (entry.get("package_name") or "").strip()
    class_name = (entry.get("class_name") or "").strip()
    member_name = (entry.get("member_name") or "").strip()
    signature_url = (entry.get("signature_url") or "").strip()

    owner = f"{package_name}.{class_name}" if package_name else class_name
    if include_urls and signature_url:
        return f"- MEMBER {owner}#{member_name} | {signature_url}"
    return f"- MEMBER {owner}#{member_name}"


def _clamp(value: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(value, maximum))


def _run_subprocess(cmd: list[str]) -> dict[str, Any]:
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(ROOT),
            stdin=subprocess.DEVNULL,
            capture_output=True,
            text=True,
            check=False,
            timeout=QUERY_TIMEOUT_SECONDS,
        )
    except subprocess.TimeoutExpired as exc:
        return {
            "ok": False,
            "error": "Command timed out",
            "timeout_seconds": QUERY_TIMEOUT_SECONDS,
            "stdout": (exc.stdout or "").strip(),
            "stderr": (exc.stderr or "").strip(),
            "command": " ".join(cmd),
        }

    return {
        "ok": proc.returncode == 0,
        "returncode": proc.returncode,
        "stdout": proc.stdout.strip(),
        "stderr": proc.stderr.strip(),
        "command": " ".join(cmd),
    }


def _run_query(
    query: str,
    version: str,
    types_limit: int,
    members_limit: int,
    as_json: bool,
) -> dict[str, Any]:
    if not INDEX_SCRIPT.exists():
        return {
            "ok": False,
            "error": f"Index script not found: {INDEX_SCRIPT}",
        }

    cmd = [
        sys.executable,
        str(INDEX_SCRIPT),
        "query",
        "--version",
        version,
        "--q",
        query,
        "--types-limit",
        str(types_limit),
        "--members-limit",
        str(members_limit),
    ]
    if as_json:
        cmd.append("--json")

    payload = _run_subprocess(cmd)

    if as_json and payload["ok"]:
        marker = "=== JSON ==="
        idx = payload["stdout"].find(marker)
        if idx != -1:
            json_blob = payload["stdout"][idx + len(marker) :].strip()
            try:
                payload["data"] = json.loads(json_blob)
            except json.JSONDecodeError:
                payload["data_parse_error"] = "Failed to parse JSON section from query output"

    return payload


@mcp.tool()
def query_fabric_api(
    query: str,
    version: str = DEFAULT_VERSION,
    types_limit: int = 6,
    members_limit: int = 10,
    as_json: bool = True,
) -> dict[str, Any]:
    """Query the local Fabric API index for symbol/member hits.

    Parameters:
      query: natural language or symbol-like query text.
      version: Fabric API docs version in the local index.
      types_limit: max class/type hits.
      members_limit: max member/signature hits.
      as_json: include parsed JSON payload when available.
    """

    return _run_query(
        query=query,
        version=version,
        types_limit=_clamp(types_limit, 1, 50),
        members_limit=_clamp(members_limit, 1, 100),
        as_json=as_json,
    )


@mcp.tool()
def query_fabric_api_compact(
    query: str,
    version: str = DEFAULT_VERSION,
    max_types: int = 4,
    max_members: int = 8,
    max_chars: int = 2200,
    include_urls: bool = False,
) -> dict[str, Any]:
    """Query local index and return a compact prompt-ready context block.

    This is optimized for low-token LLM context injection.
    """

    clamped_types = _clamp(max_types, 1, 20)
    clamped_members = _clamp(max_members, 1, 40)
    clamped_chars = _clamp(max_chars, 400, 10000)

    payload = _run_query(
        query=query,
        version=version,
        types_limit=clamped_types,
        members_limit=clamped_members,
        as_json=True,
    )

    if not payload.get("ok"):
        return {
            "ok": False,
            "error": "Query failed",
            "details": payload,
        }

    data = payload.get("data")
    if not isinstance(data, dict):
        return {
            "ok": False,
            "error": "Query returned no structured data",
            "details": payload,
        }

    type_hits = data.get("types") or []
    member_hits = data.get("members") or []
    if not isinstance(type_hits, list):
        type_hits = []
    if not isinstance(member_hits, list):
        member_hits = []

    selected_types = type_hits[:clamped_types]
    selected_members = member_hits[:clamped_members]

    lines: list[str] = [
        f"Fabric API context (version {version})",
        f"Query: {query}",
        "Top type hits:",
    ]
    lines.extend(_format_type_hit(entry, include_urls=include_urls) for entry in selected_types)

    lines.append("Top member hits:")
    lines.extend(_format_member_hit(entry, include_urls=include_urls) for entry in selected_members)

    context_block = "\n".join(lines).strip()
    truncated = False
    if len(context_block) > clamped_chars:
        context_block = context_block[: max(0, clamped_chars - 15)].rstrip() + "\n...[truncated]"
        truncated = True

    return {
        "ok": True,
        "query": query,
        "version": version,
        "selected_types": len(selected_types),
        "selected_members": len(selected_members),
        "truncated": truncated,
        "context_block": context_block,
    }


def _run_tutorial_query(
    query: str,
    limit: int,
    as_json: bool,
) -> dict[str, Any]:
    if not TUTORIAL_INDEX_SCRIPT.exists():
        return {
            "ok": False,
            "error": f"Tutorial index script not found: {TUTORIAL_INDEX_SCRIPT}",
        }

    cmd = [
        sys.executable,
        str(TUTORIAL_INDEX_SCRIPT),
        "query",
        "--q",
        query,
        "--limit",
        str(limit),
    ]
    if as_json:
        cmd.append("--json")

    payload = _run_subprocess(cmd)

    if as_json and payload.get("ok"):
        marker = "=== JSON ==="
        output = payload.get("stdout", "")
        idx = output.find(marker)
        if idx != -1:
            json_blob = output[idx + len(marker) :].strip()
            try:
                payload["data"] = json.loads(json_blob)
            except json.JSONDecodeError:
                payload["data_parse_error"] = "Failed to parse JSON section from tutorial query output"

    return payload


def _format_tutorial_hit(entry: dict[str, Any], include_urls: bool) -> str:
    title = (entry.get("title") or "").strip()
    url = (entry.get("url") or "").strip()
    snippet = (entry.get("snippet") or "").strip()

    if include_urls and url:
        header = f"- GUIDE {title} | {url}"
    else:
        header = f"- GUIDE {title}"
    if snippet:
        return f"{header}\n  Snippet: {snippet}"
    return header


@mcp.tool()
def query_fabric_tutorials(
    query: str,
    limit: int = 8,
    as_json: bool = True,
) -> dict[str, Any]:
    """Query local Fabric tutorial docs index for how-to pages."""

    return _run_tutorial_query(
        query=query,
        limit=_clamp(limit, 1, 25),
        as_json=as_json,
    )


@mcp.tool()
def query_fabric_tutorials_compact(
    query: str,
    max_hits: int = 6,
    max_chars: int = 2400,
    include_urls: bool = True,
) -> dict[str, Any]:
    """Query tutorial index and return compact prompt-ready how-to context."""

    clamped_hits = _clamp(max_hits, 1, 20)
    clamped_chars = _clamp(max_chars, 500, 12000)

    payload = _run_tutorial_query(
        query=query,
        limit=clamped_hits,
        as_json=True,
    )
    if not payload.get("ok"):
        return {
            "ok": False,
            "error": "Tutorial query failed",
            "details": payload,
        }

    data = payload.get("data")
    if not isinstance(data, dict):
        return {
            "ok": False,
            "error": "Tutorial query returned no structured data",
            "details": payload,
        }

    results = data.get("results") or []
    if not isinstance(results, list):
        results = []

    selected = results[:clamped_hits]

    lines: list[str] = [
        "Fabric tutorial context",
        f"Query: {query}",
        "Top how-to hits:",
    ]
    lines.extend(_format_tutorial_hit(entry, include_urls=include_urls) for entry in selected)

    context_block = "\n".join(lines).strip()
    truncated = False
    if len(context_block) > clamped_chars:
        context_block = context_block[: max(0, clamped_chars - 15)].rstrip() + "\n...[truncated]"
        truncated = True

    return {
        "ok": True,
        "query": query,
        "selected_hits": len(selected),
        "truncated": truncated,
        "context_block": context_block,
    }


@mcp.tool()
def sync_fabric_tutorial_index(
    seed_url: str = DEFAULT_TUTORIAL_SEED_URL,
    include_prefix: str = DEFAULT_TUTORIAL_INCLUDE_PREFIX,
    max_pages: int = 250,
    max_depth: int = 5,
    recreate: bool = False,
) -> dict[str, Any]:
    """Sync local Fabric tutorial docs index by crawling docs.fabricmc.net pages."""

    if not TUTORIAL_INDEX_SCRIPT.exists():
        return {
            "ok": False,
            "error": f"Tutorial index script not found: {TUTORIAL_INDEX_SCRIPT}",
        }

    cmd = [
        sys.executable,
        str(TUTORIAL_INDEX_SCRIPT),
        "sync",
        "--seed-url",
        seed_url,
        "--include-prefix",
        include_prefix,
        "--max-pages",
        str(_clamp(max_pages, 1, 2000)),
        "--max-depth",
        str(_clamp(max_depth, 0, 12)),
    ]
    if recreate:
        cmd.append("--recreate")

    return _run_subprocess(cmd)


@mcp.tool()
def sync_fabric_api_index(
    version: str = DEFAULT_VERSION,
    recreate: bool = False,
    download_pages: bool = False,
) -> dict[str, Any]:
    """Sync local Fabric API index by crawling search indexes and rebuilding SQLite."""

    if not INDEX_SCRIPT.exists():
        return {
            "ok": False,
            "error": f"Index script not found: {INDEX_SCRIPT}",
        }

    cmd = [
        sys.executable,
        str(INDEX_SCRIPT),
        "sync",
        "--version",
        version,
    ]
    if recreate:
        cmd.append("--recreate")
    if download_pages:
        cmd.append("--download-pages")

    return _run_subprocess(cmd)


if __name__ == "__main__":
    mcp.run()

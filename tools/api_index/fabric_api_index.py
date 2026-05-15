#!/usr/bin/env python3
"""Local Fabric Javadoc crawler + SQLite index builder + query CLI.

Usage examples:
  python tools/api_index/fabric_api_index.py sync --version 0.149.0+26.1.2
  python tools/api_index/fabric_api_index.py query --q "EntityType.Builder createMob"
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import sqlite3
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any

BASE_URL_TEMPLATE = "https://maven.fabricmc.net/docs/fabric-api-{version}/"
SEARCH_FILES = [
    "type-search-index.js",
    "member-search-index.js",
    "package-search-index.js",
]


@dataclass
class Paths:
    root: pathlib.Path
    version: str

    @property
    def docs_dir(self) -> pathlib.Path:
        return self.root / "API Documents" / "fabric-api" / self.version

    @property
    def raw_dir(self) -> pathlib.Path:
        return self.docs_dir / "raw"

    @property
    def pages_dir(self) -> pathlib.Path:
        return self.docs_dir / "pages"

    @property
    def db_path(self) -> pathlib.Path:
        return self.root / "API Documents" / "index" / f"fabric_api_{self.version}.sqlite"


def fetch_text(url: str, timeout: int = 30) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "clay-legion-api-indexer/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode("utf-8", errors="replace")


def extract_js_array(text: str) -> list[dict[str, Any]]:
    start = text.find("[")
    end = text.rfind("]")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("Could not find JSON array in search-index JS file")
    payload = text[start : end + 1]
    return json.loads(payload)


def build_class_url(base_url: str, package_name: str, class_name: str) -> str:
    package_path = package_name.replace(".", "/")
    return urllib.parse.urljoin(base_url, f"{package_path}/{class_name}.html")


def crawl(args: argparse.Namespace) -> int:
    root = pathlib.Path(args.root).resolve()
    paths = Paths(root=root, version=args.version)
    base_url = BASE_URL_TEMPLATE.format(version=args.version)

    paths.raw_dir.mkdir(parents=True, exist_ok=True)
    if args.download_pages:
        paths.pages_dir.mkdir(parents=True, exist_ok=True)

    for file_name in SEARCH_FILES:
        url = urllib.parse.urljoin(base_url, file_name)
        try:
            text = fetch_text(url)
        except urllib.error.URLError as exc:
            print(f"ERROR: Failed to fetch {url}: {exc}", file=sys.stderr)
            return 1

        out_path = paths.raw_dir / file_name
        out_path.write_text(text, encoding="utf-8")
        print(f"Saved {out_path}")

    if not args.download_pages:
        return 0

    type_index_path = paths.raw_dir / "type-search-index.js"
    type_entries = extract_js_array(type_index_path.read_text(encoding="utf-8"))

    downloaded = 0
    skipped = 0
    for entry in type_entries:
        package_name = entry.get("p")
        class_name = entry.get("l")
        if not package_name or not class_name:
            skipped += 1
            continue

        url = build_class_url(base_url, package_name, class_name)
        rel_path = pathlib.Path(package_name.replace(".", "/")) / f"{class_name}.html"
        out_path = paths.pages_dir / rel_path
        out_path.parent.mkdir(parents=True, exist_ok=True)

        try:
            text = fetch_text(url)
        except urllib.error.URLError:
            skipped += 1
            continue

        out_path.write_text(text, encoding="utf-8")
        downloaded += 1

    print(f"Downloaded class pages: {downloaded}, skipped: {skipped}")
    return 0


def init_db(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        PRAGMA journal_mode=WAL;
        PRAGMA synchronous=NORMAL;

        CREATE TABLE IF NOT EXISTS metadata (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS types (
            id INTEGER PRIMARY KEY,
            package_name TEXT,
            class_name TEXT NOT NULL,
            kind_code TEXT,
            target_url TEXT,
            raw_json TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS members (
            id INTEGER PRIMARY KEY,
            package_name TEXT,
            class_name TEXT,
            member_name TEXT NOT NULL,
            signature_url TEXT,
            kind_code TEXT,
            raw_json TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS packages (
            id INTEGER PRIMARY KEY,
            package_name TEXT NOT NULL,
            target_url TEXT,
            raw_json TEXT NOT NULL
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS types_fts USING fts5(
            package_name,
            class_name,
            target_url,
            raw_json
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS members_fts USING fts5(
            package_name,
            class_name,
            member_name,
            signature_url,
            raw_json
        );
        """
    )


def rebuild_fts(conn: sqlite3.Connection) -> None:
    conn.execute("DELETE FROM types_fts")
    conn.execute("DELETE FROM members_fts")

    conn.execute(
        """
        INSERT INTO types_fts(package_name, class_name, target_url, raw_json)
        SELECT COALESCE(package_name, ''), class_name, COALESCE(target_url, ''), raw_json
        FROM types
        """
    )

    conn.execute(
        """
        INSERT INTO members_fts(package_name, class_name, member_name, signature_url, raw_json)
        SELECT COALESCE(package_name, ''), COALESCE(class_name, ''), member_name, COALESCE(signature_url, ''), raw_json
        FROM members
        """
    )


def build(args: argparse.Namespace) -> int:
    root = pathlib.Path(args.root).resolve()
    paths = Paths(root=root, version=args.version)
    paths.db_path.parent.mkdir(parents=True, exist_ok=True)

    if getattr(args, "recreate", False):
        for candidate in (
            paths.db_path,
            pathlib.Path(str(paths.db_path) + "-wal"),
            pathlib.Path(str(paths.db_path) + "-shm"),
        ):
            if candidate.exists():
                candidate.unlink()

    raw_files = {
        file_name: paths.raw_dir / file_name for file_name in SEARCH_FILES
    }
    missing = [str(path) for path in raw_files.values() if not path.exists()]
    if missing:
        print("ERROR: Missing raw search index files. Run crawl first.", file=sys.stderr)
        for item in missing:
            print(f"  - {item}", file=sys.stderr)
        return 1

    type_entries = extract_js_array(raw_files["type-search-index.js"].read_text(encoding="utf-8"))
    member_entries = extract_js_array(raw_files["member-search-index.js"].read_text(encoding="utf-8"))
    package_entries = extract_js_array(raw_files["package-search-index.js"].read_text(encoding="utf-8"))

    conn = sqlite3.connect(paths.db_path)
    try:
        init_db(conn)
        conn.execute("DELETE FROM types")
        conn.execute("DELETE FROM members")
        conn.execute("DELETE FROM packages")

        conn.executemany(
            """
            INSERT INTO types(package_name, class_name, kind_code, target_url, raw_json)
            VALUES (?, ?, ?, ?, ?)
            """,
            [
                (
                    e.get("p"),
                    e.get("l", ""),
                    e.get("k"),
                    e.get("u"),
                    json.dumps(e, ensure_ascii=True),
                )
                for e in type_entries
            ],
        )

        conn.executemany(
            """
            INSERT INTO members(package_name, class_name, member_name, signature_url, kind_code, raw_json)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            [
                (
                    e.get("p"),
                    e.get("c"),
                    e.get("l", ""),
                    e.get("u"),
                    e.get("k"),
                    json.dumps(e, ensure_ascii=True),
                )
                for e in member_entries
            ],
        )

        conn.executemany(
            """
            INSERT INTO packages(package_name, target_url, raw_json)
            VALUES (?, ?, ?)
            """,
            [
                (
                    e.get("l", ""),
                    e.get("u"),
                    json.dumps(e, ensure_ascii=True),
                )
                for e in package_entries
            ],
        )

        rebuild_fts(conn)

        conn.execute(
            "REPLACE INTO metadata(key, value) VALUES (?, ?)",
            ("fabric_api_version", args.version),
        )
        conn.commit()

        print(f"Built SQLite index: {paths.db_path}")
        print(f"Types: {len(type_entries)}")
        print(f"Members: {len(member_entries)}")
        print(f"Packages: {len(package_entries)}")
        return 0
    finally:
        conn.close()


def query_index(args: argparse.Namespace) -> int:
    root = pathlib.Path(args.root).resolve()
    paths = Paths(root=root, version=args.version)
    if not paths.db_path.exists():
        print("ERROR: Index database not found. Run sync/build first.", file=sys.stderr)
        return 1

    conn = sqlite3.connect(paths.db_path)
    conn.row_factory = sqlite3.Row
    try:
        q = args.q.strip()
        if not q:
            print("ERROR: Empty query", file=sys.stderr)
            return 1

        # FTS5 query with fallback for punctuation-heavy or malformed MATCH syntax.
        token_list = [token for token in re.split(r"\s+", q) if token]
        safe_fallback = " OR ".join(token_list)

        def run_types(match_q: str) -> list[sqlite3.Row]:
            return conn.execute(
                """
                SELECT package_name, class_name, target_url, bm25(types_fts) AS rank
                FROM types_fts
                WHERE types_fts MATCH ?
                ORDER BY rank
                LIMIT ?
                """,
                (match_q, args.types_limit),
            ).fetchall()

        def run_members(match_q: str) -> list[sqlite3.Row]:
            return conn.execute(
                """
                SELECT package_name, class_name, member_name, signature_url, bm25(members_fts) AS rank
                FROM members_fts
                WHERE members_fts MATCH ?
                ORDER BY rank
                LIMIT ?
                """,
                (match_q, args.members_limit),
            ).fetchall()

        try:
            types = run_types(q)
            members = run_members(q)
        except sqlite3.OperationalError:
            if not safe_fallback:
                raise
            types = run_types(safe_fallback)
            members = run_members(safe_fallback)

        print("=== TYPE HITS ===")
        for row in types:
            pkg = row["package_name"] or ""
            cls = row["class_name"]
            url = row["target_url"] or ""
            print(f"- {pkg}.{cls} | {url}")

        print("\n=== MEMBER HITS ===")
        for row in members:
            pkg = row["package_name"] or ""
            cls = row["class_name"] or ""
            member = row["member_name"]
            url = row["signature_url"] or ""
            print(f"- {pkg}.{cls}#{member} | {url}")

        if args.json:
            payload = {
                "types": [dict(row) for row in types],
                "members": [dict(row) for row in members],
            }
            print("\n=== JSON ===")
            print(json.dumps(payload, indent=2, ensure_ascii=True))

        return 0
    finally:
        conn.close()


def sync(args: argparse.Namespace) -> int:
    crawl_code = crawl(args)
    if crawl_code != 0:
        return crawl_code
    return build(args)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Fabric API local index tool")
    parser.add_argument(
        "--root",
        default=".",
        help="Workspace root (default: current directory)",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    parser_crawl = subparsers.add_parser("crawl", help="Download Javadoc search-index files")
    parser_crawl.add_argument("--version", required=True, help="Fabric API version string")
    parser_crawl.add_argument(
        "--download-pages",
        action="store_true",
        help="Also download class HTML pages for offline lookup",
    )
    parser_crawl.set_defaults(func=crawl)

    parser_build = subparsers.add_parser("build", help="Build SQLite index from downloaded files")
    parser_build.add_argument("--version", required=True, help="Fabric API version string")
    parser_build.add_argument(
        "--recreate",
        action="store_true",
        help="Delete and recreate the SQLite database before indexing",
    )
    parser_build.set_defaults(func=build)

    parser_sync = subparsers.add_parser("sync", help="Run crawl then build")
    parser_sync.add_argument("--version", required=True, help="Fabric API version string")
    parser_sync.add_argument(
        "--recreate",
        action="store_true",
        help="Delete and recreate the SQLite database before indexing",
    )
    parser_sync.add_argument(
        "--download-pages",
        action="store_true",
        help="Also download class HTML pages for offline lookup",
    )
    parser_sync.set_defaults(func=sync)

    parser_query = subparsers.add_parser("query", help="Query local index")
    parser_query.add_argument("--version", required=True, help="Fabric API version string")
    parser_query.add_argument("--q", required=True, help="Search query")
    parser_query.add_argument("--types-limit", type=int, default=8)
    parser_query.add_argument("--members-limit", type=int, default=12)
    parser_query.add_argument("--json", action="store_true", help="Emit JSON output")
    parser_query.set_defaults(func=query_index)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return int(args.func(args))
    except sqlite3.OperationalError as exc:
        print(f"ERROR: SQLite failure: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:  # pylint: disable=broad-except
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Local Fabric tutorial docs crawler + SQLite FTS index + query CLI.

Usage examples:
  python tools/api_index/fabric_docs_tutorial_index.py sync
  python tools/api_index/fabric_docs_tutorial_index.py query --q "screen handler tutorial"
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib
import re
import sqlite3
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import deque
from dataclasses import dataclass
from html.parser import HTMLParser
from typing import Any

DEFAULT_SEED_URL = "https://docs.fabricmc.net/develop/getting-started/creating-a-project"
DEFAULT_INCLUDE_PREFIX = "/develop/"
DEFAULT_USER_AGENT = "clay-legion-fabric-docs-indexer/1.0"


@dataclass
class Paths:
    root: pathlib.Path

    @property
    def docs_dir(self) -> pathlib.Path:
        return self.root / "API Documents" / "fabric-docs" / "tutorials"

    @property
    def db_path(self) -> pathlib.Path:
        return self.root / "API Documents" / "index" / "fabric_tutorials.sqlite"


class TutorialPageParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.links: list[str] = []
        self._text_parts: list[str] = []
        self._title_parts: list[str] = []
        self._skip_depth = 0
        self._in_title = False

    @property
    def text(self) -> str:
        joined = " ".join(self._text_parts)
        return re.sub(r"\s+", " ", joined).strip()

    @property
    def title(self) -> str:
        joined = " ".join(self._title_parts)
        return re.sub(r"\s+", " ", joined).strip()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        lowered = tag.lower()
        if lowered in {"script", "style", "noscript", "svg"}:
            self._skip_depth += 1
            return

        if lowered == "title":
            self._in_title = True

        if lowered == "a":
            for key, value in attrs:
                if key.lower() == "href" and value:
                    self.links.append(value)

    def handle_endtag(self, tag: str) -> None:
        lowered = tag.lower()
        if lowered in {"script", "style", "noscript", "svg"} and self._skip_depth > 0:
            self._skip_depth -= 1
            return

        if lowered == "title":
            self._in_title = False

    def handle_data(self, data: str) -> None:
        if self._skip_depth > 0:
            return
        value = data.strip()
        if not value:
            return
        self._text_parts.append(value)
        if self._in_title:
            self._title_parts.append(value)


def fetch_text(url: str, timeout: int = 30) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": DEFAULT_USER_AGENT})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        charset = resp.headers.get_content_charset() or "utf-8"
        return resp.read().decode(charset, errors="replace")


def canonicalize_url(url: str) -> str:
    parsed = urllib.parse.urlparse(url)
    cleaned = parsed._replace(fragment="", query="")
    normalized_path = re.sub(r"/+", "/", cleaned.path or "/")
    cleaned = cleaned._replace(path=normalized_path)
    return urllib.parse.urlunparse(cleaned)


def is_supported_doc_url(url: str, include_prefix: str) -> bool:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in {"http", "https"}:
        return False
    if parsed.netloc.lower() != "docs.fabricmc.net":
        return False

    path = parsed.path or "/"
    if include_prefix and not path.startswith(include_prefix):
        return False

    if re.search(r"\.(png|jpg|jpeg|gif|webp|svg|css|js|json|xml|ico|pdf|zip|tar|gz)$", path, flags=re.IGNORECASE):
        return False

    return True


def extract_page(html: str) -> tuple[str, str, list[str]]:
    parser = TutorialPageParser()
    parser.feed(html)
    title = parser.title
    text = parser.text
    return title, text, parser.links


def init_db(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        PRAGMA journal_mode=WAL;
        PRAGMA synchronous=NORMAL;

        CREATE TABLE IF NOT EXISTS metadata (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pages (
            id INTEGER PRIMARY KEY,
            url TEXT NOT NULL UNIQUE,
            title TEXT NOT NULL,
            section_path TEXT NOT NULL,
            content TEXT NOT NULL,
            content_length INTEGER NOT NULL,
            crawled_at TEXT NOT NULL
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS pages_fts USING fts5(
            title,
            section_path,
            url,
            content
        );
        """
    )


def rebuild_fts(conn: sqlite3.Connection) -> None:
    conn.execute("DELETE FROM pages_fts")
    conn.execute(
        """
        INSERT INTO pages_fts(title, section_path, url, content)
        SELECT title, section_path, url, content
        FROM pages
        """
    )


def crawl_tutorial_pages(seed_url: str, include_prefix: str, max_pages: int, max_depth: int) -> list[dict[str, str]]:
    queue: deque[tuple[str, int]] = deque([(canonicalize_url(seed_url), 0)])
    visited: set[str] = set()
    pages: list[dict[str, str]] = []

    while queue and len(pages) < max_pages:
        current_url, depth = queue.popleft()
        if current_url in visited:
            continue
        visited.add(current_url)

        if not is_supported_doc_url(current_url, include_prefix=include_prefix):
            continue

        try:
            html = fetch_text(current_url)
        except urllib.error.URLError:
            continue

        title, text, links = extract_page(html)
        if len(text) < 100:
            continue

        parsed = urllib.parse.urlparse(current_url)
        section_path = parsed.path or "/"
        pages.append(
            {
                "url": current_url,
                "title": title or section_path,
                "section_path": section_path,
                "content": text,
            }
        )

        if depth >= max_depth:
            continue

        for href in links:
            abs_url = canonicalize_url(urllib.parse.urljoin(current_url, href))
            if abs_url in visited:
                continue
            if not is_supported_doc_url(abs_url, include_prefix=include_prefix):
                continue
            queue.append((abs_url, depth + 1))

    return pages


def sync(args: argparse.Namespace) -> int:
    root = pathlib.Path(args.root).resolve()
    paths = Paths(root=root)
    paths.db_path.parent.mkdir(parents=True, exist_ok=True)
    paths.docs_dir.mkdir(parents=True, exist_ok=True)

    if args.recreate:
        for candidate in (
            paths.db_path,
            pathlib.Path(str(paths.db_path) + "-wal"),
            pathlib.Path(str(paths.db_path) + "-shm"),
        ):
            if candidate.exists():
                candidate.unlink()

    pages = crawl_tutorial_pages(
        seed_url=args.seed_url,
        include_prefix=args.include_prefix,
        max_pages=args.max_pages,
        max_depth=args.max_depth,
    )
    if not pages:
        print("ERROR: No tutorial pages were crawled", file=sys.stderr)
        return 1

    now_iso = dt.datetime.now(tz=dt.timezone.utc).isoformat()

    conn = sqlite3.connect(paths.db_path)
    try:
        init_db(conn)
        conn.execute("DELETE FROM pages")

        conn.executemany(
            """
            INSERT INTO pages(url, title, section_path, content, content_length, crawled_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            [
                (
                    page["url"],
                    page["title"],
                    page["section_path"],
                    page["content"],
                    len(page["content"]),
                    now_iso,
                )
                for page in pages
            ],
        )

        rebuild_fts(conn)

        conn.execute("REPLACE INTO metadata(key, value) VALUES (?, ?)", ("seed_url", args.seed_url))
        conn.execute("REPLACE INTO metadata(key, value) VALUES (?, ?)", ("include_prefix", args.include_prefix))
        conn.execute("REPLACE INTO metadata(key, value) VALUES (?, ?)", ("max_pages", str(args.max_pages)))
        conn.execute("REPLACE INTO metadata(key, value) VALUES (?, ?)", ("max_depth", str(args.max_depth)))
        conn.execute("REPLACE INTO metadata(key, value) VALUES (?, ?)", ("last_sync_utc", now_iso))

        conn.commit()
    finally:
        conn.close()

    snapshot_path = paths.docs_dir / "last_sync_snapshot.json"
    snapshot_path.write_text(
        json.dumps(
            {
                "seed_url": args.seed_url,
                "include_prefix": args.include_prefix,
                "max_pages": args.max_pages,
                "max_depth": args.max_depth,
                "pages_indexed": len(pages),
                "last_sync_utc": now_iso,
            },
            indent=2,
            ensure_ascii=True,
        ),
        encoding="utf-8",
    )

    print(f"Built tutorial index: {paths.db_path}")
    print(f"Pages indexed: {len(pages)}")
    print(f"Snapshot: {snapshot_path}")
    return 0


def safe_match_query(query: str) -> str:
    tokens = re.findall(r"[A-Za-z0-9_]{2,}", query)
    if not tokens:
        return ""
    return " AND ".join(f"{token}*" for token in tokens)


def build_snippet(content: str, query: str, max_len: int = 240) -> str:
    query_terms = [t.lower() for t in re.findall(r"[A-Za-z0-9_]{2,}", query)]
    lowered = content.lower()
    pos = -1
    for term in query_terms:
        p = lowered.find(term)
        if p != -1 and (pos == -1 or p < pos):
            pos = p

    if pos == -1:
        start = 0
    else:
        start = max(0, pos - 60)

    snippet = content[start : start + max_len].strip()
    if start > 0:
        snippet = "..." + snippet
    if start + max_len < len(content):
        snippet = snippet + "..."
    return snippet


def query_index(args: argparse.Namespace) -> int:
    root = pathlib.Path(args.root).resolve()
    paths = Paths(root=root)
    if not paths.db_path.exists():
        print("ERROR: Tutorial index database not found. Run sync first.", file=sys.stderr)
        return 1

    q = args.q.strip()
    if not q:
        print("ERROR: Empty query", file=sys.stderr)
        return 1

    conn = sqlite3.connect(paths.db_path)
    conn.row_factory = sqlite3.Row
    try:
        rows: list[sqlite3.Row]
        match_query = safe_match_query(q)
        if match_query:
            try:
                rows = conn.execute(
                    """
                    SELECT p.url, p.title, p.section_path, p.content
                    FROM pages_fts f
                    JOIN pages p ON p.rowid = f.rowid
                    WHERE pages_fts MATCH ?
                    LIMIT ?
                    """,
                    (match_query, args.limit),
                ).fetchall()
            except sqlite3.OperationalError:
                rows = []
        else:
            rows = []

        if not rows:
            like = f"%{q}%"
            rows = conn.execute(
                """
                SELECT url, title, section_path, content
                FROM pages
                WHERE title LIKE ? OR content LIKE ? OR section_path LIKE ?
                LIMIT ?
                """,
                (like, like, like, args.limit),
            ).fetchall()

        records = [
            {
                "url": row["url"],
                "title": row["title"],
                "section_path": row["section_path"],
                "snippet": build_snippet(row["content"], q),
            }
            for row in rows
        ]

        print("=== Tutorial hits ===")
        for item in records:
            print(f"- {item['title']} | {item['url']}")
            print(f"  Path: {item['section_path']}")
            print(f"  Snippet: {item['snippet']}")

        if args.json:
            print("=== JSON ===")
            print(
                json.dumps(
                    {
                        "query": q,
                        "limit": args.limit,
                        "count": len(records),
                        "results": records,
                    },
                    indent=2,
                    ensure_ascii=True,
                )
            )

        return 0
    finally:
        conn.close()


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Fabric tutorial docs index CLI")
    parser.add_argument("--root", default=".", help="Repository root path")

    sub = parser.add_subparsers(dest="command", required=True)

    sync_parser = sub.add_parser("sync", help="Crawl and rebuild tutorial index")
    sync_parser.add_argument("--seed-url", default=DEFAULT_SEED_URL)
    sync_parser.add_argument("--include-prefix", default=DEFAULT_INCLUDE_PREFIX)
    sync_parser.add_argument("--max-pages", type=int, default=250)
    sync_parser.add_argument("--max-depth", type=int, default=5)
    sync_parser.add_argument("--recreate", action="store_true")
    sync_parser.set_defaults(func=sync)

    query_parser = sub.add_parser("query", help="Query tutorial index")
    query_parser.add_argument("--q", required=True, help="Query text")
    query_parser.add_argument("--limit", type=int, default=8)
    query_parser.add_argument("--json", action="store_true")
    query_parser.set_defaults(func=query_index)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main())

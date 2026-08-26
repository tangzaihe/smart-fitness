#!/usr/bin/env python3
"""Minimal JetBrains IDEA MCP SSE client for one-shot tool calls."""

from __future__ import annotations

import json
import re
import socket
import sys
import threading
import time
import urllib.parse
import urllib.request
from http.client import HTTPResponse
from typing import Any

BASE = "http://127.0.0.1:64342"
DEFAULT_PROJECT_PATH = r"E:/aseantec/agent/Smart Fitness"


def parse_sse_events(raw: str) -> list[tuple[str | None, str]]:
    events: list[tuple[str | None, str]] = []
    event_type: str | None = None
    data_lines: list[str] = []
    for line in raw.splitlines():
        if line.startswith("event:"):
            event_type = line[5:].strip()
        elif line.startswith("data:"):
            data_lines.append(line[5:].strip())
        elif line == "" and data_lines:
            events.append((event_type, "\n".join(data_lines)))
            event_type = None
            data_lines = []
    if data_lines:
        events.append((event_type, "\n".join(data_lines)))
    return events


class IdeaMcpClient:
    def __init__(self, base: str = BASE) -> None:
        self.base = base.rstrip("/")
        self.session_id: str | None = None
        self.responses: dict[int | str, Any] = {}
        self.events: list[Any] = []
        self._lock = threading.Lock()
        self._next_id = 1
        self._resp: HTTPResponse | None = None
        self._thread: threading.Thread | None = None
        self._stop = threading.Event()

    def connect(self) -> None:
        req = urllib.request.Request(
            f"{self.base}/sse",
            headers={"Accept": "text/event-stream", "Cache-Control": "no-cache"},
            method="GET",
        )
        self._resp = urllib.request.urlopen(req, timeout=30)
        # SSE is a long-lived stream; avoid blocking read(n) waiting for more bytes.
        self._resp.fp.raw._sock.settimeout(5.0)  # type: ignore[attr-defined]
        chunk = self._read_available_sse_bytes(max_wait_s=8.0)
        for event_type, data in parse_sse_events(chunk):
            if "sessionId=" in data:
                self.session_id = data.split("sessionId=", 1)[1].strip()
        if not self.session_id:
            match = re.search(r"sessionId=([0-9a-f-]+)", chunk, re.I)
            if match:
                self.session_id = match.group(1)
        if not self.session_id:
            raise RuntimeError(f"Failed to obtain MCP session from SSE: {chunk!r}")
        self._resp.fp.raw._sock.settimeout(2.0)  # type: ignore[attr-defined]
        self._thread = threading.Thread(target=self._read_loop, daemon=True)
        self._thread.start()
        time.sleep(0.1)

    def _read_available_sse_bytes(self, max_wait_s: float = 5.0) -> str:
        assert self._resp is not None
        deadline = time.time() + max_wait_s
        parts: list[str] = []
        while time.time() < deadline:
            try:
                piece = self._resp.read1(4096)  # type: ignore[attr-defined]
            except AttributeError:
                piece = self._resp.read(4096)
            if piece:
                parts.append(piece.decode("utf-8", errors="replace"))
                joined = "".join(parts)
                if "sessionId=" in joined:
                    return joined
            else:
                time.sleep(0.05)
        return "".join(parts)

    def _drain_sse_blocks(self, buf: str) -> tuple[list[tuple[str | None, str]], str]:
        events: list[tuple[str | None, str]] = []
        normalized = buf.replace("\r\n", "\n")
        while "\n\n" in normalized:
            block, normalized = normalized.split("\n\n", 1)
            events.extend(parse_sse_events(block + "\n\n"))
        return events, normalized

    def _read_loop(self) -> None:
        assert self._resp is not None
        buf = ""
        while not self._stop.is_set():
            try:
                try:
                    piece = self._resp.read1(4096)  # type: ignore[attr-defined]
                except AttributeError:
                    piece = self._resp.read(4096)
                except socket.timeout:
                    continue
                if not piece:
                    time.sleep(0.05)
                    continue
                buf += piece.decode("utf-8", errors="replace")
                blocks, buf = self._drain_sse_blocks(buf)
                for event_type, data in blocks:
                    if event_type != "message" and not data.startswith("{"):
                        continue
                    try:
                        payload = json.loads(data)
                    except json.JSONDecodeError:
                        continue
                    with self._lock:
                        self.events.append(payload)
                        if "id" in payload:
                            self.responses[payload["id"]] = payload
            except socket.timeout:
                continue
            except Exception:
                break

    def post(self, message: dict[str, Any]) -> int | str | None:
        if not self.session_id:
            raise RuntimeError("Not connected")
        if "id" not in message:
            message = dict(message)
            message["id"] = self._next_id
            self._next_id += 1
        data = json.dumps(message).encode("utf-8")
        url = f"{self.base}/message?sessionId={urllib.parse.quote(self.session_id)}"
        req = urllib.request.Request(
            url,
            data=data,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        urllib.request.urlopen(req, timeout=30).read()
        return message["id"]

    def request(self, method: str, params: dict[str, Any] | None = None, timeout: float = 15.0) -> Any:
        msg_id = self.post(
            {
                "jsonrpc": "2.0",
                "method": method,
                "params": params or {},
            }
        )
        deadline = time.time() + timeout
        while time.time() < deadline:
            with self._lock:
                if msg_id in self.responses:
                    return self.responses.pop(msg_id)
            time.sleep(0.05)
        raise TimeoutError(f"Timeout waiting for {method} response")

    def close(self) -> None:
        self._stop.set()
        if self._resp is not None:
            try:
                self._resp.close()
            except Exception:
                pass


def main() -> int:
    tool = sys.argv[1] if len(sys.argv) > 1 else "tools/list"
    if len(sys.argv) > 2:
        if sys.argv[2] == "-":
            args = json.load(sys.stdin)
        else:
            args = json.loads(sys.argv[2])
    else:
        args = {}
    if tool != "tools/list" and "projectPath" not in args:
        args["projectPath"] = DEFAULT_PROJECT_PATH

    client = IdeaMcpClient()
    client.connect()
    try:
        init = client.request(
            "initialize",
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "cursor-script", "version": "1.0"},
            },
        )
        print("INIT_OK", json.dumps(init, ensure_ascii=False)[:500])
        client.post({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})

        if tool == "tools/list":
            result = client.request("tools/list", {})
            print(json.dumps(result, ensure_ascii=False, indent=2))
            return 0

        timeout = 180.0 if tool in {"build_project", "execute_run_configuration"} else 15.0
        result = client.request("tools/call", {"name": tool, "arguments": args}, timeout=timeout)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0
    finally:
        client.close()


if __name__ == "__main__":
    raise SystemExit(main())

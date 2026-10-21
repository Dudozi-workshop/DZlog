#!/usr/bin/env python3
"""Stream a subprocess log while enforcing a wall-clock timeout on its process group."""
import os
from pathlib import Path
import signal
import subprocess
import sys
import threading


def main() -> int:
    if len(sys.argv) < 4:
        print("Usage: run-capped-command.py SECONDS LOG_FILE COMMAND [ARGS...]", file=sys.stderr)
        return 2
    limit = int(sys.argv[1])
    if limit <= 0:
        raise ValueError("SECONDS must be positive")
    log_path = Path(sys.argv[2])
    command = sys.argv[3:]
    log_path.parent.mkdir(parents=True, exist_ok=True)
    process = subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, errors="replace", bufsize=1, start_new_session=True,
    )

    def stream_output() -> None:
        with log_path.open("w", encoding="utf-8") as log:
            assert process.stdout is not None
            for line in process.stdout:
                sys.stdout.write(line)
                sys.stdout.flush()
                log.write(line)
                log.flush()

    reader = threading.Thread(target=stream_output, daemon=True)
    reader.start()
    try:
        status = process.wait(timeout=limit)
    except subprocess.TimeoutExpired:
        warning = f"UI_TEST_TIMEOUT: exceeded {limit}s; terminating xcodebuild process group"
        print(warning, file=sys.stderr, flush=True)
        with log_path.open("a", encoding="utf-8") as log:
            log.write(warning + "\n")
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        try:
            process.wait(timeout=8)
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()
        status = 124
    reader.join(timeout=8)
    if reader.is_alive():
        print("OUTPUT_READER_STALLED: log may be incomplete", file=sys.stderr, flush=True)
    return status


if __name__ == "__main__":
    sys.exit(main())

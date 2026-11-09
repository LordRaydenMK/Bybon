#!/usr/bin/env python3
import json
import sys
import urllib.error
import urllib.request

HUB_URL = "http://127.0.0.1:4400/emulators"
REQUIRED = ("auth", "firestore")


def main() -> int:
    try:
        with urllib.request.urlopen(HUB_URL, timeout=5) as response:
            data = json.load(response)
    except urllib.error.URLError as error:
        print(f"Failed to reach Firebase emulator hub at {HUB_URL}: {error}", file=sys.stderr)
        return 1

    print(json.dumps(data, indent=2))
    missing = [name for name in REQUIRED if name not in data]
    if missing:
        print(
            f"Firebase emulator hub is missing {missing}; got {sorted(data)}",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

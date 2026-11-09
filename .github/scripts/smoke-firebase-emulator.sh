#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
import json
import sys
import urllib.request

with urllib.request.urlopen("http://127.0.0.1:4400/emulators", timeout=5) as response:
    data = json.load(response)

print(json.dumps(data, indent=2))
missing = [name for name in ("auth", "firestore") if name not in data]
if missing:
    sys.exit(f"Firebase emulator hub is missing {missing}; got {sorted(data)}")
PY

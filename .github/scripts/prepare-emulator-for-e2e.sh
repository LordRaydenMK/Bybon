#!/usr/bin/env bash
set -euo pipefail

# CI emulators (API 36 + swiftshader + Pixel 7 Pro) occasionally ANR System UI.
# The resulting "isn't responding" dialog covers the app and fails Maestro
# asserts even though the app itself rendered. Hide those dialogs and dismiss
# one if it is already on screen.
# See https://github.com/actions/runner-images/issues/3719
adb shell settings put global hide_error_dialogs 1
adb shell settings put secure anr_show_background 0

window_dump="$(adb shell dumpsys window 2>/dev/null || true)"
if printf '%s' "${window_dump}" | grep -Eq 'Application Error|Not Responding|aerr_wait'; then
  echo "Dismissing existing ANR/error dialog"
  adb shell input keyevent KEYCODE_DPAD_DOWN
  adb shell input keyevent KEYCODE_ENTER
fi

#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
csv="${repo_root}/app/src/test/resources/strong-backup-sample.csv"
dest_dir="/storage/emulated/0/Download"
dest="${dest_dir}/strong-backup-sample.csv"

adb shell mkdir -p "${dest_dir}"
adb push "${csv}" "${dest}"
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://${dest}"

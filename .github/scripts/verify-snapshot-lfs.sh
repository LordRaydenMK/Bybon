#!/usr/bin/env bash
# Fail if Paparazzi snapshot PNGs are not stored as Git LFS objects.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT}"

SNAPSHOT_DIR="app/src/test/snapshots"
GITATTRIBUTES=".gitattributes"
PATTERN="app/src/test/snapshots/**/*.png"

if ! command -v git-lfs >/dev/null 2>&1; then
  echo "git-lfs is not installed. Install Git LFS and run 'git lfs install'." >&2
  exit 1
fi

if [[ ! -f "${GITATTRIBUTES}" ]]; then
  echo "Missing ${GITATTRIBUTES}; expected LFS tracking for ${PATTERN}" >&2
  exit 1
fi

if ! grep -Eq '^app/src/test/snapshots/\*\*/\*\.png[[:space:]]+filter=lfs' "${GITATTRIBUTES}"; then
  echo "${GITATTRIBUTES} must track ${PATTERN} with filter=lfs" >&2
  exit 1
fi

mapfile -t snapshot_files < <(find "${SNAPSHOT_DIR}" -type f -name '*.png' | sort || true)

if [[ ${#snapshot_files[@]} -eq 0 ]]; then
  echo "No snapshot PNGs under ${SNAPSHOT_DIR}; LFS attributes are configured."
  exit 0
fi

failed=0
for file in "${snapshot_files[@]}"; do
  filter="$(git check-attr filter -- "${file}" | awk -F': ' '{print $NF}')"
  if [[ "${filter}" != "lfs" ]]; then
    echo "${file} is not tracked by Git LFS (filter=${filter})" >&2
    failed=1
    continue
  fi

  blob_path="${file}"
  if git cat-file -e "HEAD:${blob_path}" 2>/dev/null; then
    pointer_source="HEAD:${blob_path}"
  elif git cat-file -e ":${blob_path}" 2>/dev/null; then
    pointer_source=":${blob_path}"
  else
    continue
  fi

  first_line="$(git cat-file -p "${pointer_source}" | head -n 1)"
  if [[ "${first_line}" != "version https://git-lfs.github.com/spec/v1" ]]; then
    echo "${file} is stored as a Git blob, not an LFS pointer" >&2
    failed=1
  fi
done

if [[ "${failed}" -ne 0 ]]; then
  echo "Snapshot LFS verification failed. Track PNGs with Git LFS and re-add them." >&2
  exit 1
fi

echo "Verified ${#snapshot_files[@]} snapshot PNG(s) are tracked by Git LFS."

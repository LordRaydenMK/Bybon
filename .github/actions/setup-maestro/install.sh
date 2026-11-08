#!/usr/bin/env bash
set -euo pipefail

# Download Maestro from GitHub Releases via `gh`. Browser asset URLs (used by
# get.maestro.mobile.dev) 504 on the large zip; the Releases API does not.
MAESTRO_VERSION="${MAESTRO_VERSION:?MAESTRO_VERSION is required}"
MAESTRO_VERSION="${MAESTRO_VERSION#cli-}"
MAESTRO_DIR="${MAESTRO_DIR:-${HOME}/.maestro}"
REPO="mobile-dev-inc/maestro"
TMP="${MAESTRO_DIR}/tmp"
mkdir -p "${TMP}"

bin="${MAESTRO_DIR}/bin/maestro"
if [[ -x "${bin}" ]]; then
  installed="$("${bin}" --version 2>/dev/null || true)"
  if [[ "${installed}" == *"${MAESTRO_VERSION}"* ]]; then
    echo "Maestro ${MAESTRO_VERSION} already installed at ${bin}"
    exit 0
  fi
fi

if ! command -v gh >/dev/null; then
  echo "gh is required to install Maestro from GitHub Releases" >&2
  exit 1
fi

download_asset() {
  local pattern="$1"
  local attempt
  for attempt in 1 2 3 4 5; do
    if gh release download "cli-${MAESTRO_VERSION}" \
      --repo "${REPO}" \
      --pattern "${pattern}" \
      --dir "${TMP}" \
      --clobber; then
      return 0
    fi
    echo "Download of ${pattern} failed (attempt ${attempt})" >&2
    sleep $((attempt * 5))
  done
  echo "Failed to download ${pattern} for Maestro ${MAESTRO_VERSION}" >&2
  return 1
}

echo "Installing Maestro CLI ${MAESTRO_VERSION} from GitHub Releases"
download_asset "checksums_sha256.txt"
download_asset "maestro.zip"

zip_file="${TMP}/maestro.zip"
sum_file="${TMP}/checksums_sha256.txt"
expected="$(awk '/[[:space:]]maestro\.zip$/ { print $1; exit }' "${sum_file}")"
if [[ -z "${expected}" ]]; then
  echo "Could not read maestro.zip checksum from ${sum_file}" >&2
  cat "${sum_file}" >&2
  exit 1
fi
actual="$(sha256sum "${zip_file}" | awk '{ print $1 }')"
if [[ "${actual}" != "${expected}" ]]; then
  echo "Checksum mismatch for maestro.zip" >&2
  echo "expected ${expected}" >&2
  echo "got      ${actual}" >&2
  exit 1
fi

echo "Extracting Maestro ${MAESTRO_VERSION}"
rm -rf "${TMP}/maestro"
unzip -qo "${zip_file}" -d "${TMP}"
rm -rf "${MAESTRO_DIR}/lib" "${MAESTRO_DIR}/bin"
cp -rf "${TMP}/maestro/." "${MAESTRO_DIR}"
rm -rf "${TMP}"

if [[ ! -x "${bin}" ]]; then
  echo "Maestro is not installed at ${bin}" >&2
  exit 1
fi

"${bin}" --version

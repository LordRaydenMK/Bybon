#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for the Bybon Android project.
# - Installs the Android SDK command-line tools + the packages the build needs.
# - Points Gradle at the SDK via local.properties.
# - Warms the Gradle/dependency caches so later builds are fast.
#
# The platform package is read from compileSdk in app/build.gradle.kts so a
# minor API bump (for example release(37) { minorApiLevel = 1 }) installs
# platforms;android-37.1 instead of a stale android-37.0.
#   bash .cursor/install.sh --print-platform
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="15859902"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}"

# Latest stable Build-Tools. AGP's own default (currently 36.0.0) is fetched
# during the Gradle warmup; compileSdk is what pins the platform package.
BUILD_TOOLS_PKG="build-tools;37.0.0"
PLATFORM_TOOLS_PKG="platform-tools"

log() { printf '\n\033[1;34m[install]\033[0m %s\n' "$*"; }

# Map compileSdk in a Gradle Kotlin script to an sdkmanager platform id.
#   compileSdk { version = release(37) { minorApiLevel = 1 } } -> platforms;android-37.1
#   compileSdk { version = release(37) }                       -> platforms;android-37.0
#   compileSdk = 36                                            -> platforms;android-36
resolve_platform_pkg() {
  local gradle_file="$1"
  if [[ ! -f "${gradle_file}" ]]; then
    printf 'Missing Gradle file: %s\n' "${gradle_file}" >&2
    return 1
  fi
  python3 - "${gradle_file}" <<'PY'
import re
import sys

path = sys.argv[1]
text = open(path, encoding="utf-8").read()
text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
text = re.sub(r"//.*?$", "", text, flags=re.M)


def package(major, minor):
    # From API 37, AGP's platform hash includes the minor even when it is 0.
    if minor is None and major >= 37:
        minor = 0
    if minor is None:
        return f"platforms;android-{major}"
    return f"platforms;android-{major}.{minor}"


def compile_sdk_block(source):
    match = re.search(r"\bcompileSdk\s*\{", source)
    if not match:
        return None
    depth = 1
    index = match.end()
    while index < len(source) and depth:
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
        index += 1
    if depth != 0:
        sys.exit(f"Unbalanced compileSdk block in {path}")
    return source[match.end() : index - 1]


block = compile_sdk_block(text)
if block is not None:
    release = re.search(r"\brelease\s*\(\s*(\d+)\s*\)", block)
    if not release:
        sys.exit(f"compileSdk block in {path} has no release(apiLevel)")
    major = int(release.group(1))
    minor_match = re.search(r"\bminorApiLevel\s*=\s*(\d+)", block)
    minor = int(minor_match.group(1)) if minor_match else None
    print(package(major, minor))
    sys.exit(0)

assigned = re.search(r"\bcompileSdk\s*=\s*(\d+)\b", text)
if assigned:
    print(package(int(assigned.group(1)), None))
    sys.exit(0)

sys.exit(f"Could not find compileSdk in {path}")
PY
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_GRADLE="${REPO_ROOT}/app/build.gradle.kts"
PLATFORM_PKG="$(resolve_platform_pkg "${APP_GRADLE}")"

if [[ "${1:-}" == "--print-platform" ]]; then
  printf '%s\n' "${PLATFORM_PKG}"
  exit 0
fi

log "Ensuring Android SDK command-line tools are present at ${ANDROID_SDK_ROOT}"
SDKMANAGER="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "${SDKMANAGER}" ]]; then
  log "Downloading Android command-line tools (${CMDLINE_TOOLS_VERSION})"
  tmp_zip="$(mktemp --suffix=.zip)"
  curl -fsSL "${CMDLINE_TOOLS_URL}" -o "${tmp_zip}"
  mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
  rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest" "${ANDROID_SDK_ROOT}/cmdline-tools/tmp-unzip"
  unzip -q "${tmp_zip}" -d "${ANDROID_SDK_ROOT}/cmdline-tools/tmp-unzip"
  mv "${ANDROID_SDK_ROOT}/cmdline-tools/tmp-unzip/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  rmdir "${ANDROID_SDK_ROOT}/cmdline-tools/tmp-unzip"
  rm -f "${tmp_zip}"
else
  log "Command-line tools already installed, skipping download"
fi

export ANDROID_SDK_ROOT
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

log "Accepting SDK licenses"
yes | sdkmanager --licenses >/dev/null 2>&1 || true

log "Installing SDK packages from ${APP_GRADLE}: ${PLATFORM_TOOLS_PKG}, ${PLATFORM_PKG}, ${BUILD_TOOLS_PKG}"
sdkmanager --install \
  "${PLATFORM_TOOLS_PKG}" \
  "${PLATFORM_PKG}" \
  "${BUILD_TOOLS_PKG}" >/dev/null

log "Writing ${REPO_ROOT}/local.properties"
printf 'sdk.dir=%s\n' "${ANDROID_SDK_ROOT}" > "${REPO_ROOT}/local.properties"

log "Warming Gradle build (assembleDebug also validates Room/KSP codegen)"
cd "${REPO_ROOT}"
./gradlew --no-daemon assembleDebug

log "Install complete. ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT} platform=${PLATFORM_PKG}"

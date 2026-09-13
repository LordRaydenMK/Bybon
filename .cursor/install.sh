#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for the Bybon Android project.
# - Installs the Android SDK command-line tools + the packages the build needs.
# - Points Gradle at the SDK via local.properties.
# - Warms the Gradle/dependency caches so later builds are fast.
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="15859902"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}"

# SDK packages required by app/build.gradle.kts (compileSdk/targetSdk 37) and CI.
PLATFORM_PKG="platforms;android-37.0"
BUILD_TOOLS_PKG="build-tools;37.0.0"
PLATFORM_TOOLS_PKG="platform-tools"

log() { printf '\n\033[1;34m[install]\033[0m %s\n' "$*"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

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

log "Installing SDK packages: ${PLATFORM_TOOLS_PKG}, ${PLATFORM_PKG}, ${BUILD_TOOLS_PKG}"
sdkmanager --install \
  "${PLATFORM_TOOLS_PKG}" \
  "${PLATFORM_PKG}" \
  "${BUILD_TOOLS_PKG}" >/dev/null

log "Writing ${REPO_ROOT}/local.properties"
printf 'sdk.dir=%s\n' "${ANDROID_SDK_ROOT}" > "${REPO_ROOT}/local.properties"

log "Warming Gradle build (assembleDebug also validates Room/KSP codegen)"
cd "${REPO_ROOT}"
./gradlew --no-daemon assembleDebug

log "Install complete. ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT}"

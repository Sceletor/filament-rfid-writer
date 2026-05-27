#!/usr/bin/env bash
# Capture Play Store screenshots via Compose UI tests.
# Writes PNG files to /sdcard/DCIM/ via MediaStore (survives teardown).
#
# Usage:
#   ./scripts/capture-screenshots.sh              # dark mode (default)
#   ./scripts/capture-screenshots.sh --theme light
#   ./scripts/capture-screenshots.sh --theme dark
set -euo pipefail

APP_PKG="com.blumlaut.filamenttagwriter"
TEST_CLASS="com.blumlaut.filamenttagwriter.ScreenshotTest"
TEST_PKG="${APP_PKG}.test"
DCIM_DIR="/sdcard/DCIM/Screenshots"
OUTPUT_DIR="./docs"

ADB="/opt/android-sdk/platform-tools/adb"
GRADLE="./gradlew"

# Parse args
THEME="dark"
for arg in "$@"; do
  case "$arg" in
    --theme) shift; THEME="${1:-dark}" ;;
  esac
done

if [[ "$THEME" != "light" && "$THEME" != "dark" ]]; then
  echo "Error: --theme must be 'light' or 'dark' (got '$THEME')"
  exit 1
fi

echo "=== Capturing screenshots (theme: $THEME) ==="

# Build
$GRADLE :app:assembleDebug :app:assembleAndroidTest --no-configuration-cache -q

# Install
$ADB install -r "app/build/outputs/apk/debug/app-debug.apk"
$ADB install -r "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

# Set system theme
if [[ "$THEME" == "dark" ]]; then
  $ADB shell cmd uimode night yes
else
  $ADB shell cmd uimode night no
fi
sleep 1

# Run test (pass theme as argument so the test can suffix filenames)
$ADB shell am instrument \
  -w \
  -e class "$TEST_CLASS" \
  -e theme "$THEME" \
  "$TEST_PKG/androidx.test.runner.AndroidJUnitRunner" || true

# Restore system theme
if [[ "$THEME" == "dark" ]]; then
  $ADB shell cmd uimode night no
fi

# Pull screenshots
mkdir -p "$OUTPUT_DIR"
SUFFIX="_$THEME"
for f in printer read catalog write form; do
  REMOTE="${DCIM_DIR}/screenshot_${f}${SUFFIX}.png"
  LOCAL="${OUTPUT_DIR}/screenshot_${f}${SUFFIX}.png"
  if $ADB shell "[ -f $REMOTE ]" 2>/dev/null; then
    $ADB pull "$REMOTE" "$LOCAL"
    echo "  Pulled: $LOCAL ($(stat -c%s "$LOCAL" 2>/dev/null) bytes)"
  else
    echo "  MISSING: $REMOTE"
  fi
done

# Cleanup
$ADB shell "rm -f ${DCIM_DIR}/screenshot_*.png" 2>/dev/null || true
$ADB uninstall "$TEST_PKG" 2>/dev/null || true

echo "=== Done ==="
ls -la "$OUTPUT_DIR"/screenshot_*"${SUFFIX}".png 2>/dev/null

#!/usr/bin/env bash
# Capture Play Store screenshots via Compose UI tests.
# Writes PNG files to /sdcard/DCIM/ so they survive test teardown.
set -euo pipefail

APP_PKG="com.blumlaut.filamenttagwriter"
TEST_CLASS="com.blumlaut.filamenttagwriter.ScreenshotTest"
TEST_PKG="${APP_PKG}.test"
DCIM_DIR="/sdcard/DCIM/Screenshots"
OUTPUT_DIR="./docs"

ADB="/opt/android-sdk/platform-tools/adb"
GRADLE="./gradlew"

echo "=== Building debug + test APKs ==="
$GRADLE :app:assembleDebug :app:assembleAndroidTest --no-configuration-cache -q

echo "=== Installing APKs ==="
$ADB install -r "app/build/outputs/apk/debug/app-debug.apk"
$ADB install -r "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

echo "=== Running screenshot test ==="
# am instrument returns when test finishes but does NOT uninstall the APKs
$ADB shell am instrument \
  -w \
  -e class "$TEST_CLASS" \
  "$TEST_PKG/androidx.test.runner.AndroidJUnitRunner" || true

echo "=== Pulling screenshots ==="
mkdir -p "$OUTPUT_DIR"
for f in printer read catalog write form; do
  REMOTE="${DCIM_DIR}/screenshot_${f}.png"
  LOCAL="${OUTPUT_DIR}/screenshot_${f}.png"
  if $ADB shell "[ -f $REMOTE ]" 2>/dev/null; then
    $ADB pull "$REMOTE" "$LOCAL"
    echo "  Pulled: $LOCAL ($(stat -c%s "$LOCAL" 2>/dev/null || stat -f%z "$LOCAL" 2>/dev/null) bytes)"
  else
    echo "  MISSING: $REMOTE"
  fi
done

echo "=== Cleaning up ==="
# Remove remote files
$ADB shell "rm -f ${DCIM_DIR}/screenshot_*.png" 2>/dev/null || true
# Uninstall test APK (main app stays installed)
$ADB uninstall "$TEST_PKG" 2>/dev/null || true

echo "=== Done ==="
ls -la "$OUTPUT_DIR"/screenshot_*.png 2>/dev/null

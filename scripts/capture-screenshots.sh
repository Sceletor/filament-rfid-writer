#!/bin/bash
# Captures app screenshots via Compose UI tests (base64 via logcat)
# and generates Play Store-ready assets.
#
# Usage: ./scripts/capture-screenshots.sh
# Prerequisites: running emulator, ImageMagick (convert)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_DIR/docs"
TEST_CLASS="com.blumlaut.filamenttagwriter.ScreenshotTest"
LOG_FILE="/tmp/screenshot_logs.txt"

cd "$PROJECT_DIR"

# --- Step 1: Run UI tests ---
echo "=== Running screenshot UI tests ==="
adb logcat -c 2>/dev/null || true
./gradlew :app:connectedAndroidTest --no-configuration-cache \
  -PtestInstrumentationRunnerArguments.class=$TEST_CLASS 2>&1 | tail -3

# --- Step 2: Extract base64 screenshots from logcat ---
echo ""
echo "=== Extracting screenshots from logcat ==="
adb logcat -d | grep "ScreenshotTest" > "$LOG_FILE"

for screen in read catalog printer form; do
  echo -n "  Decoding screenshot_${screen}.png... "
  sed -n "/SCREENSHOT_START:${screen}/,/SCREENSHOT_END:${screen}/p" "$LOG_FILE" | \
    grep -v "SCREENSHOT_START\|SCREENSHOT_END" | \
    sed 's/.*ScreenshotTest: //' | \
    tr -d '\n' | \
    base64 -d > "$DOCS_DIR/screenshot_${screen}.png" 2>/dev/null
  size=$(wc -c < "$DOCS_DIR/screenshot_${screen}.png" 2>/dev/null || echo "0")
  if [ "$size" -gt 1000 ]; then
    echo "OK ($size bytes)"
  else
    echo "FAILED ($size bytes)"
    exit 1
  fi
done

# --- Step 3: Resize to Play Store dimensions (1080x2412) ---
# Current screenshots are 1080x2340. Add 72px padding (36 top + 36 bottom).
echo ""
echo "=== Resizing to Play Store dimensions ==="
if command -v convert &>/dev/null; then
  for screen in read catalog printer form; do
    echo -n "  Resizing screenshot_${screen}.png... "
    convert "$DOCS_DIR/screenshot_${screen}.png" \
      -background "#F5F5F5" \
      -gravity center \
      -extent 1080x2412 \
      "$DOCS_DIR/playstore_screenshot_${screen}.png"
    echo "OK (1080x2412)"
  done
else
  echo "  ImageMagick not found — skipping resize (install with: sudo pacman -S imagemagick)"
fi

# --- Step 4: Generate feature graphic (1024x500) ---
echo ""
echo "=== Generating feature graphic ==="
if command -v convert &>/dev/null; then
  # Composite: logo on left, catalog screenshot on right, with title text
  LOGO="$DOCS_DIR/logo.png"
  CATALOG="$DOCS_DIR/screenshot_catalog.png"

  if [ -f "$LOGO" ] && [ -f "$CATALOG" ]; then
    # Resize catalog screenshot to fit feature graphic
    convert "$CATALOG" -resize 600x800! -gravity center -extent 600x400 \
      "$DOCS_DIR/tmp_catalog_fg.png"

    # Resize logo
    convert "$LOGO" -resize 200x200 \
      "$DOCS_DIR/tmp_logo_fg.png"

    # Composite: white background + logo + catalog + title
    convert \
      -size 1024x500 xc:"#E0E7FF" \
      "$DOCS_DIR/tmp_logo_fg.png" -gravity northwest -geometry +80+150 \
      "$DOCS_DIR/tmp_catalog_fg.png" -gravity southeast -geometry +80+60 \
      -gravity center \
      -pointsize 48 -fill "#1E293B" -annotate 0 "Filament RFID Writer" \
      "$DOCS_DIR/feature-graphic-1024x500.png"

    rm -f "$DOCS_DIR/tmp_catalog_fg.png" "$DOCS_DIR/tmp_logo_fg.png"
    echo "  Generated feature-graphic-1024x500.png"
  else
    echo "  Logo or catalog screenshot missing — skipping feature graphic"
  fi
else
  echo "  ImageMagick not found — skipping feature graphic"
fi

# Cleanup
rm -f "$LOG_FILE"

echo ""
echo "=== Done! Assets in $DOCS_DIR ==="
ls -lh "$DOCS_DIR"/screenshot_*.png "$DOCS_DIR"/playstore_screenshot_*.png "$DOCS_DIR"/feature-graphic-*.png 2>/dev/null

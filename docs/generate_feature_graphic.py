#!/usr/bin/env python3
"""Generate a Play Store feature graphic (1024x500) from the logo and a dark catalog screenshot.

Usage:
    python3 generate_feature_graphic.py

Inputs  (relative to this script's directory):
    logo_512.png              – app icon (512x512)
    screenshot_catalog_dark.png – dark-mode catalog screenshot (1080x2340)

Output:
    feature-graphic-1024x500.png
"""

import math
import os

from PIL import Image, ImageDraw, ImageFont

# ── Paths ────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
LOGO_PATH = os.path.join(SCRIPT_DIR, "logo_512.png")
SCREENSHOT_PATH = os.path.join(SCRIPT_DIR, "screenshot_catalog_dark.png")
OUTPUT_PATH = os.path.join(SCRIPT_DIR, "feature-graphic-1024x500.png")

# ── Font paths (adjust if needed) ───────────────────────────────────────────
FONT_BOLD = "/usr/share/fonts/TTF/Roboto-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/TTF/Roboto-Regular.ttf"

# ── Dimensions ──────────────────────────────────────────────────────────────
CANVAS_W, CANVAS_H = 1024, 500

# ── Layout constants ────────────────────────────────────────────────────────
LOGO_SIZE = 140
LOGO_X = 60
LOGO_Y = (CANVAS_H - LOGO_SIZE) // 2  # vertically centred

TITLE_TEXT = "Filament RFID Writer"
TITLE_SIZE = 44
SUBTITLE_TEXT = "Read, write & catalog NTAG213 tags"
SUBTITLE_SIZE = 22

# Crop the catalog card area from the screenshot so it displays larger.
# Coordinates are in the original 1080x2340 screenshot.
SCREENSHOT_CROP = (0, 260, 1080, 1550)  # title + search + cards through PLA White
SCREENSHOT_MARGIN_RIGHT = 40
SCREENSHOT_MARGIN_V = 50
SCREENSHOT_CORNER_RADIUS = 28


def radial_gradient(width, height, center_color, edge_color):
    """Create a subtle radial gradient image."""
    img = Image.new("RGB", (width, height), edge_color)
    draw = ImageDraw.Draw(img)
    cx, cy = width // 2, height // 2
    max_r = math.ceil(math.sqrt(cx ** 2 + cy ** 2))
    for r in range(max_r, 0, -1):
        ratio = r / max_r
        # Interpolate colour
        c = tuple(int(center_color[i] * (1 - ratio) + edge_color[i] * ratio)
                  for i in range(3))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=c)
    return img


def rounded_rect(mask_img, radius):
    """Create a rounded-rectangle mask for compositing."""
    mask = Image.new("L", mask_img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        [0, 0, mask_img.size[0], mask_img.size[1]],
        radius=radius,
        fill=255,
    )
    return mask


def main():
    # ── 1. Background ────────────────────────────────────────────────────
    bg = radial_gradient(
        CANVAS_W, CANVAS_H,
        center_color=(27, 20, 100),   # #1B1464  (brand purple)
        edge_color=(9, 7, 30),        # very dark purple
    )

    # ── 2. Logo ──────────────────────────────────────────────────────────
    logo = Image.open(LOGO_PATH).resize((LOGO_SIZE, LOGO_SIZE), Image.LANCZOS)
    # Remove the solid background from the logo so it sits on the gradient.
    # The logo background is #1B1464 – alpha-mat it onto the gradient.
    logo_rgba = logo.convert("RGBA")
    bg_rgb = (27, 20, 100)
    # Distance-based alpha: pixels close to bg colour become transparent,
    # pixels far away stay opaque. Smooth transition avoids halo artifacts.
    threshold = 60       # exact match → alpha 0
    fade_range = 40      # beyond this distance → alpha 255
    data = logo_rgba.load()
    w, h = logo_rgba.size
    for x in range(w):
        for y in range(h):
            r, g, b, _a = data[x, y]
            dist = math.sqrt((r - bg_rgb[0]) ** 2 +
                             (g - bg_rgb[1]) ** 2 +
                             (b - bg_rgb[2]) ** 2)
            if dist <= threshold:
                alpha = 0
            elif dist >= threshold + fade_range:
                alpha = 255
            else:
                alpha = int(255 * (dist - threshold) / fade_range)
            data[x, y] = (r, g, b, alpha)
    bg.paste(logo_rgba, (LOGO_X, LOGO_Y), logo_rgba)

    # ── 3. Title + subtitle ──────────────────────────────────────────────
    draw = ImageDraw.Draw(bg)
    font_title = ImageFont.truetype(FONT_BOLD, TITLE_SIZE)
    font_sub = ImageFont.truetype(FONT_REGULAR, SUBTITLE_SIZE)

    # Position text to the right of the logo
    text_x = LOGO_X + LOGO_SIZE + 32
    title_bbox = draw.textbbox((0, 0), TITLE_TEXT, font=font_title)
    title_h = title_bbox[3] - title_bbox[1]
    subtitle_bbox = draw.textbbox((0, 0), SUBTITLE_TEXT, font=font_sub)
    subtitle_h = subtitle_bbox[3] - subtitle_bbox[1]
    total_text_h = title_h + 8 + subtitle_h  # 8px gap
    text_y = (CANVAS_H - total_text_h) // 2

    draw.text((text_x, text_y), TITLE_TEXT, fill=(255, 255, 255), font=font_title)
    draw.text(
        (text_x, text_y + title_h + 8),
        SUBTITLE_TEXT,
        fill=(196, 181, 253),  # soft purple tint
        font=font_sub,
    )

    # ── 4. Screenshot ────────────────────────────────────────────────────
    screenshot = Image.open(SCREENSHOT_PATH)
    if SCREENSHOT_CROP:
        screenshot = screenshot.crop(SCREENSHOT_CROP)

    # Scale screenshot to fit the right side of the banner
    aspect = screenshot.size[0] / screenshot.size[1]
    # Target: fit within vertical margins, cap width so it doesn't overlap text
    max_h = CANVAS_H - 2 * SCREENSHOT_MARGIN_V
    max_w = int((CANVAS_W - SCREENSHOT_MARGIN_RIGHT - 600) * 0.85)  # 15% smaller
    scaled_w = max_w
    scaled_h = int(round(scaled_w / aspect))
    if scaled_h > max_h:
        scaled_h = max_h
        scaled_w = int(round(scaled_h * aspect))
    screenshot = screenshot.resize((int(scaled_w), int(scaled_h)), Image.LANCZOS)

    # Composite with rounded corners
    mask = rounded_rect(screenshot, SCREENSHOT_CORNER_RADIUS)
    screenshot_rgba = screenshot.convert("RGBA")
    screenshot_rgba.putalpha(mask)

    sx = CANVAS_W - scaled_w - SCREENSHOT_MARGIN_RIGHT
    sy = (CANVAS_H - scaled_h) // 2  # vertically centre
    bg.paste(screenshot_rgba, (sx, sy), screenshot_rgba)

    # ── 5. Save ──────────────────────────────────────────────────────────
    bg = bg.convert("RGB")
    bg.save(OUTPUT_PATH, "PNG")
    print(f"Written → {OUTPUT_PATH}  ({bg.size[0]}x{bg.size[1]})")


if __name__ == "__main__":
    main()
